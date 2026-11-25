import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../core/api_client.dart';
import '../models/models.dart';

final secureStorageProvider = Provider<FlutterSecureStorage>((ref) => const FlutterSecureStorage());

/// État d'authentification : `initializing` pendant la lecture de la session stockée au démarrage.
class AuthState {
  const AuthState({this.initializing = false, this.session});

  final bool initializing;
  final UserSession? session;
}

class AuthNotifier extends Notifier<AuthState> {
  static const _storageKey = 'ekyc_session';

  @override
  AuthState build() {
    Future.microtask(_restore);
    return const AuthState(initializing: true);
  }

  Future<void> _restore() async {
    UserSession? session;
    try {
      final raw = await ref.read(secureStorageProvider).read(key: _storageKey);
      if (raw != null) {
        final stored = UserSession.fromJson(jsonDecode(raw) as Map<String, dynamic>);
        if (!stored.isExpired) session = stored;
      }
    } catch (_) {
      // Stockage illisible : on repart sur l'écran de connexion.
    }
    state = AuthState(session: session);
  }

  Future<void> login(String email, String password) async {
    final session = await ref.read(apiClientProvider).login(email.trim().toLowerCase(), password);
    await _open(session);
  }

  Future<void> register(String fullName, String email, String password) async {
    final session = await ref.read(apiClientProvider).register(fullName.trim(), email.trim().toLowerCase(), password);
    await _open(session);
  }

  Future<void> _open(UserSession session) async {
    if (session.role != 'CLIENT') {
      throw const ApiException('Ce compte est réservé au back-office. Connecte-toi avec un compte client.');
    }
    try {
      await ref.read(secureStorageProvider).write(key: _storageKey, value: jsonEncode(session.toJson()));
    } catch (_) {
      // Pas bloquant : la session reste valable tant que l'app est ouverte.
    }
    state = AuthState(session: session);
  }

  Future<void> logout() async {
    state = const AuthState();
    try {
      await ref.read(secureStorageProvider).delete(key: _storageKey);
    } catch (_) {}
  }
}

final authProvider = NotifierProvider<AuthNotifier, AuthState>(AuthNotifier.new);

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient(
      getToken: () => ref.read(authProvider).session?.token,
      onUnauthorized: () => ref.read(authProvider.notifier).logout(),
    ));

/// Mes dossiers. Se recharge quand l'utilisateur change.
final casesProvider = FutureProvider.autoDispose<List<KycCase>>((ref) {
  ref.watch(authProvider.select((s) => s.session?.email));
  return ref.watch(apiClientProvider).listCases();
});

/// Un dossier par identifiant.
final caseProvider = FutureProvider.autoDispose.family<KycCase, int>((ref, id) {
  return ref.watch(apiClientProvider).getCase(id);
});
