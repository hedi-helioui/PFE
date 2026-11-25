import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/format.dart';
import '../state/providers.dart';
import 'case_detail_screen.dart';

/// Formulaire d'identité déclarée : c'est ce que l'OCR comparera au document.
class NewCaseScreen extends ConsumerStatefulWidget {
  const NewCaseScreen({super.key});

  @override
  ConsumerState<NewCaseScreen> createState() => _NewCaseScreenState();
}

class _NewCaseScreenState extends ConsumerState<NewCaseScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _idNumber = TextEditingController();

  DateTime? _birthDate;
  bool _dateMissing = false;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _idNumber.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _birthDate ?? DateTime(now.year - 25, now.month, now.day),
      firstDate: DateTime(1900),
      lastDate: now.subtract(const Duration(days: 1)),
      helpText: 'Date de naissance',
    );
    if (picked != null) {
      setState(() {
        _birthDate = picked;
        _dateMissing = false;
      });
    }
  }

  Future<void> _submit() async {
    final formOk = _formKey.currentState!.validate();
    if (_birthDate == null) setState(() => _dateMissing = true);
    if (!formOk || _birthDate == null) return;

    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final created = await ref.read(apiClientProvider).createCase(
            fullName: _name.text.trim(),
            idNumber: _idNumber.text.trim(),
            birthDateIso: toIsoDate(_birthDate!),
          );
      ref.invalidate(casesProvider);
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => CaseDetailScreen(caseId: created.id)),
      );
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'Une erreur est survenue.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Nouveau dossier')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Saisis ton identité exactement comme sur ta pièce d\'identité. '
                  'Elle sera comparée automatiquement au document que tu enverras.',
                  style: theme.textTheme.bodyMedium,
                ),
                const SizedBox(height: 20),
                TextFormField(
                  controller: _name,
                  textCapitalization: TextCapitalization.words,
                  decoration: const InputDecoration(labelText: 'Nom et prénom', border: OutlineInputBorder()),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Nom requis' : null,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _idNumber,
                  decoration: const InputDecoration(
                    labelText: 'Numéro CIN / passeport',
                    border: OutlineInputBorder(),
                  ),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Numéro requis' : null,
                ),
                const SizedBox(height: 12),
                InkWell(
                  onTap: _pickDate,
                  child: InputDecorator(
                    decoration: InputDecoration(
                      labelText: 'Date de naissance',
                      border: const OutlineInputBorder(),
                      suffixIcon: const Icon(Icons.calendar_today),
                      errorText: _dateMissing ? 'Date requise' : null,
                    ),
                    child: Text(_birthDate == null ? 'Choisir une date' : formatDate(_birthDate!)),
                  ),
                ),
                if (_error != null) ...[
                  const SizedBox(height: 12),
                  Text(_error!, style: TextStyle(color: theme.colorScheme.error)),
                ],
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: _loading ? null : _submit,
                  child: _loading
                      ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('Continuer'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
