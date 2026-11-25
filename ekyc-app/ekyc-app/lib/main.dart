import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'screens/auth_screen.dart';
import 'screens/cases_screen.dart';
import 'state/providers.dart';

final navigatorKey = GlobalKey<NavigatorState>();

void main() {
  runApp(const ProviderScope(child: EkycApp()));
}

class EkycApp extends StatelessWidget {
  const EkycApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'E-KYC',
      navigatorKey: navigatorKey,
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.indigo),
      home: const AuthGate(),
    );
  }
}

/// Affiche la connexion ou la liste des dossiers selon l'état d'authentification.
class AuthGate extends ConsumerWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Déconnexion (manuelle ou session expirée) : on ferme les écrans ouverts par-dessus.
    ref.listen<AuthState>(authProvider, (previous, next) {
      if (previous?.session != null && next.session == null) {
        navigatorKey.currentState?.popUntil((route) => route.isFirst);
      }
    });

    final auth = ref.watch(authProvider);
    if (auth.initializing) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return auth.session == null ? const AuthScreen() : const CasesScreen();
  }
}
