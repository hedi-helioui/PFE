import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../core/api_client.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../state/providers.dart';
import '../widgets/status_chip.dart';

/// Un dossier : étapes d'enrôlement tant qu'il est en brouillon, résultat ensuite.
class CaseDetailScreen extends ConsumerStatefulWidget {
  const CaseDetailScreen({super.key, required this.caseId});

  final int caseId;

  @override
  ConsumerState<CaseDetailScreen> createState() => _CaseDetailScreenState();
}

class _CaseDetailScreenState extends ConsumerState<CaseDetailScreen> {
  final _picker = ImagePicker();
  String _docType = 'CIN'; // CIN | PASSPORT
  String? _busy; // libellé de l'opération en cours (désactive les boutons)

  ApiClient get _api => ref.read(apiClientProvider);

  void _snack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  /// Exécute une opération réseau avec indicateur de chargement, puis recharge le dossier.
  Future<void> _run(String label, Future<void> Function() action) async {
    setState(() => _busy = label);
    try {
      await action();
      if (!mounted) return;
      ref.invalidate(caseProvider(widget.caseId));
      ref.invalidate(casesProvider);
    } on ApiException catch (e) {
      _snack(e.message);
    } catch (_) {
      _snack('Une erreur est survenue.');
    } finally {
      if (mounted) setState(() => _busy = null);
    }
  }

  Future<void> _takeSelfie() async {
    final file = await _picker.pickImage(
      source: ImageSource.camera,
      preferredCameraDevice: CameraDevice.front,
      maxWidth: 1600,
      imageQuality: 85,
    );
    if (file == null) return;
    await _run('Envoi du selfie…', () => _api.uploadSelfie(widget.caseId, file));
  }

  Future<void> _recordVideo() async {
    final file = await _picker.pickVideo(
      source: ImageSource.camera,
      preferredCameraDevice: CameraDevice.front,
      maxDuration: const Duration(seconds: 10),
    );
    if (file == null) return;
    await _run('Envoi de la vidéo…', () => _api.uploadVideo(widget.caseId, file));
  }

  Future<void> _addDocument() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_camera),
              title: const Text('Prendre une photo'),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Choisir dans la galerie'),
              onTap: () => Navigator.pop(context, ImageSource.gallery),
            ),
          ],
        ),
      ),
    );
    if (source == null) return;
    final file = await _picker.pickImage(source: source, maxWidth: 2200, imageQuality: 90);
    if (file == null) return;
    await _run('Envoi du document…', () => _api.uploadDocument(widget.caseId, _docType, file));
  }

  Future<void> _submit() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Soumettre le dossier ?'),
        content: const Text('Tu ne pourras plus modifier tes pièces après l\'envoi.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Soumettre')),
        ],
      ),
    );
    if (confirmed != true) return;
    await _run('Analyse en cours…', () => _api.submit(widget.caseId));
  }

  @override
  Widget build(BuildContext context) {
    final caseAsync = ref.watch(caseProvider(widget.caseId));

    return Scaffold(
      appBar: AppBar(title: Text('Dossier n° ${widget.caseId}')),
      body: caseAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(error is ApiException ? error.message : 'Chargement impossible.', textAlign: TextAlign.center),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => ref.invalidate(caseProvider(widget.caseId)),
                  child: const Text('Réessayer'),
                ),
              ],
            ),
          ),
        ),
        data: (kycCase) => ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _Header(kycCase),
            const SizedBox(height: 16),
            if (kycCase.status == CaseStatus.pending) ..._enrollment(kycCase) else _ResultCard(kycCase),
          ],
        ),
      ),
    );
  }

  List<Widget> _enrollment(KycCase c) {
    final enabled = _busy == null;
    return [
      if (_busy != null)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Row(children: [
            const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2)),
            const SizedBox(width: 12),
            Text(_busy!),
          ]),
        ),
      _StepTile(
        icon: Icons.face,
        title: '1. Selfie',
        subtitle: 'Visage bien éclairé, de face, sans lunettes de soleil.',
        done: c.hasSelfie,
        actionLabel: c.hasSelfie ? 'Refaire' : 'Prendre',
        onPressed: enabled ? _takeSelfie : null,
      ),
      _StepTile(
        icon: Icons.videocam,
        title: '2. Vidéo de vérification',
        subtitle: '10 secondes maximum : regarde la caméra et tourne doucement la tête.',
        done: c.hasVideo,
        actionLabel: c.hasVideo ? 'Refaire' : 'Filmer',
        onPressed: enabled ? _recordVideo : null,
      ),
      _StepTile(
        icon: Icons.badge,
        title: '3. Pièce d\'identité',
        subtitle: 'Photo nette du document entier, sans reflets.',
        done: c.hasDocument,
        actionLabel: c.hasDocument ? 'Remplacer' : 'Ajouter',
        onPressed: enabled ? _addDocument : null,
        extra: SegmentedButton<String>(
          segments: const [
            ButtonSegment(value: 'CIN', label: Text('CIN')),
            ButtonSegment(value: 'PASSPORT', label: Text('Passeport')),
          ],
          selected: {_docType},
          onSelectionChanged: enabled ? (s) => setState(() => _docType = s.first) : null,
        ),
      ),
      const SizedBox(height: 16),
      FilledButton.icon(
        icon: const Icon(Icons.send),
        label: const Text('Soumettre le dossier'),
        onPressed: (c.isComplete && enabled) ? _submit : null,
      ),
      if (!c.isComplete)
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Text(
            'Complète les 3 étapes pour pouvoir soumettre.',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ),
    ];
  }
}

class _Header extends StatelessWidget {
  const _Header(this.c);

  final KycCase c;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Identité déclarée', style: Theme.of(context).textTheme.titleSmall),
                StatusChip(c.status),
              ],
            ),
            const SizedBox(height: 8),
            Text(c.declaredFullName, style: Theme.of(context).textTheme.titleMedium),
            Text('N° ${c.declaredIdNumber}'),
            Text('Né(e) le ${formatDate(c.declaredBirthDate)}'),
          ],
        ),
      ),
    );
  }
}

class _StepTile extends StatelessWidget {
  const _StepTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.done,
    required this.actionLabel,
    required this.onPressed,
    this.extra,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final bool done;
  final String actionLabel;
  final VoidCallback? onPressed;
  final Widget? extra;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(done ? Icons.check_circle : icon, color: done ? Colors.green : theme.colorScheme.primary),
                const SizedBox(width: 12),
                Expanded(child: Text(title, style: theme.textTheme.titleMedium)),
                OutlinedButton(onPressed: onPressed, child: Text(actionLabel)),
              ],
            ),
            const SizedBox(height: 6),
            Text(subtitle, style: theme.textTheme.bodySmall),
            if (extra != null) ...[
              const SizedBox(height: 12),
              extra!,
            ],
          ],
        ),
      ),
    );
  }
}

/// Résultat une fois le dossier soumis (statut, commentaire de l'agent, détail des scores).
class _ResultCard extends StatelessWidget {
  const _ResultCard(this.c);

  final KycCase c;

  String get _message => switch (c.status) {
        CaseStatus.approved => 'Ton identité a été vérifiée. Merci !',
        CaseStatus.rejected => 'Ta vérification a été refusée.',
        CaseStatus.manualReview => 'Ton dossier est en cours de vérification par un agent.',
        CaseStatus.processing => 'Ton dossier est en cours d\'analyse.',
        CaseStatus.pending => '',
      };

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final result = c.result;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_message, style: theme.textTheme.titleMedium),
            if (c.decisionComment != null && c.decisionComment!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('Commentaire de l\'agent : ${c.decisionComment}'),
            ],
            if (result != null) ...[
              const Divider(height: 32),
              Text('Détail de la vérification', style: theme.textTheme.titleSmall),
              const SizedBox(height: 12),
              _Score('Correspondance du visage', result.faceMatchScore),
              _Score('Détection de vivacité', result.livenessScore),
              _Score('Cohérence des informations', result.consistencyScore),
              _Score('Risque de fraude documentaire', result.fraudScore, lowerIsBetter: true),
            ],
          ],
        ),
      ),
    );
  }
}

class _Score extends StatelessWidget {
  const _Score(this.label, this.value, {this.lowerIsBetter = false});

  final String label;
  final double value;
  final bool lowerIsBetter;

  @override
  Widget build(BuildContext context) {
    final good = lowerIsBetter ? value <= 0.2 : value >= 0.8;
    final bad = lowerIsBetter ? value >= 0.6 : value < 0.5;
    final color = good ? Colors.green : (bad ? Colors.red : Colors.orange);
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [Text(label), Text('${(value * 100).round()} %')],
          ),
          const SizedBox(height: 4),
          LinearProgressIndicator(value: value.clamp(0.0, 1.0), color: color, minHeight: 6),
        ],
      ),
    );
  }
}
