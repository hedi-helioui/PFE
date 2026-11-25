import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/format.dart';
import '../state/providers.dart';
import '../widgets/status_chip.dart';
import 'case_detail_screen.dart';
import 'new_case_screen.dart';

/// Liste de mes dossiers KYC.
class CasesScreen extends ConsumerWidget {
  const CasesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cases = ref.watch(casesProvider);
    final name = ref.watch(authProvider.select((s) => s.session?.fullName)) ?? '';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mes dossiers'),
        actions: [
          IconButton(
            tooltip: 'Se déconnecter',
            icon: const Icon(Icons.logout),
            onPressed: () => ref.read(authProvider.notifier).logout(),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: const Text('Nouveau dossier'),
        onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const NewCaseScreen())),
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(casesProvider);
          try {
            await ref.read(casesProvider.future);
          } catch (_) {
            // L'erreur est déjà affichée par l'état du provider.
          }
        },
        child: cases.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, _) => _Message(
            icon: Icons.cloud_off,
            text: error is ApiException ? error.message : 'Chargement impossible.',
            actionLabel: 'Réessayer',
            onAction: () => ref.invalidate(casesProvider),
          ),
          data: (list) {
            if (list.isEmpty) {
              return _Message(
                icon: Icons.folder_open,
                text: 'Bonjour $name,\nvous n\'avez pas encore de dossier.\nCommencez votre vérification d\'identité.',
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: list.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final c = list[index];
                return Card(
                  child: ListTile(
                    title: Text('Dossier n° ${c.id}'),
                    subtitle: Text('${c.declaredFullName}\n${formatDateTime(c.createdAt)}'),
                    isThreeLine: true,
                    trailing: StatusChip(c.status),
                    onTap: () => Navigator.of(context)
                        .push(MaterialPageRoute(builder: (_) => CaseDetailScreen(caseId: c.id))),
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }
}

/// Message centré, dans un ListView pour rester compatible avec le pull-to-refresh.
class _Message extends StatelessWidget {
  const _Message({required this.icon, required this.text, this.actionLabel, this.onAction});

  final IconData icon;
  final String text;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(32),
      children: [
        const SizedBox(height: 80),
        Icon(icon, size: 56, color: Theme.of(context).colorScheme.outline),
        const SizedBox(height: 16),
        Text(text, textAlign: TextAlign.center),
        if (actionLabel != null) ...[
          const SizedBox(height: 16),
          Center(child: OutlinedButton(onPressed: onAction, child: Text(actionLabel!))),
        ],
      ],
    );
  }
}
