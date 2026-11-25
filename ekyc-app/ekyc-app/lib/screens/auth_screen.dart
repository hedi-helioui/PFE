import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../state/providers.dart';

/// Connexion et création de compte client sur le même écran.
class AuthScreen extends ConsumerStatefulWidget {
  const AuthScreen({super.key});

  @override
  ConsumerState<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends ConsumerState<AuthScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();

  bool _registerMode = false;
  bool _loading = false;
  bool _hidePassword = true;
  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final auth = ref.read(authProvider.notifier);
      if (_registerMode) {
        await auth.register(_name.text, _email.text, _password.text);
      } else {
        await auth.login(_email.text, _password.text);
      }
      // Succès : AuthGate affiche automatiquement la liste des dossiers.
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
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Icon(Icons.verified_user_outlined, size: 64, color: theme.colorScheme.primary),
                    const SizedBox(height: 12),
                    Text('Vérification d\'identité',
                        textAlign: TextAlign.center, style: theme.textTheme.headlineSmall),
                    const SizedBox(height: 4),
                    Text(
                      _registerMode ? 'Crée ton compte client' : 'Connecte-toi pour continuer',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodyMedium,
                    ),
                    const SizedBox(height: 24),
                    if (_registerMode) ...[
                      TextFormField(
                        controller: _name,
                        textCapitalization: TextCapitalization.words,
                        decoration: const InputDecoration(labelText: 'Nom complet', border: OutlineInputBorder()),
                        validator: (v) => (v == null || v.trim().isEmpty) ? 'Nom requis' : null,
                      ),
                      const SizedBox(height: 12),
                    ],
                    TextFormField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      autocorrect: false,
                      decoration: const InputDecoration(labelText: 'Email', border: OutlineInputBorder()),
                      validator: (v) =>
                          (v == null || !RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(v.trim())) ? 'Email invalide' : null,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _password,
                      obscureText: _hidePassword,
                      decoration: InputDecoration(
                        labelText: 'Mot de passe',
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          icon: Icon(_hidePassword ? Icons.visibility : Icons.visibility_off),
                          onPressed: () => setState(() => _hidePassword = !_hidePassword),
                        ),
                      ),
                      validator: (v) {
                        if (v == null || v.isEmpty) return 'Mot de passe requis';
                        if (_registerMode && v.length < 8) return '8 caractères minimum';
                        return null;
                      },
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 12),
                      Text(_error!, style: TextStyle(color: theme.colorScheme.error)),
                    ],
                    const SizedBox(height: 20),
                    FilledButton(
                      onPressed: _loading ? null : _submit,
                      child: _loading
                          ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : Text(_registerMode ? 'Créer mon compte' : 'Se connecter'),
                    ),
                    TextButton(
                      onPressed: _loading
                          ? null
                          : () => setState(() {
                                _registerMode = !_registerMode;
                                _error = null;
                              }),
                      child: Text(_registerMode ? 'J\'ai déjà un compte' : 'Créer un compte'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
