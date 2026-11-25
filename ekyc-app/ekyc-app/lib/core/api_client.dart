import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';
import 'package:image_picker/image_picker.dart';

import '../models/models.dart';
import 'config.dart';

/// Erreur lisible renvoyée par l'API (ou réseau), affichable telle quelle à l'utilisateur.
class ApiException implements Exception {
  const ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

/// Client REST du backend E-KYC (Dio).
class ApiClient {
  ApiClient({
    required String? Function() getToken,
    required void Function() onUnauthorized,
    String? baseUrl,
  })  : _getToken = getToken,
        _onUnauthorized = onUnauthorized,
        _dio = Dio(BaseOptions(
          baseUrl: baseUrl ?? AppConfig.apiBaseUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 60),
          sendTimeout: const Duration(minutes: 3),
          headers: {'Accept': 'application/json'},
        )) {
    _dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) {
      final token = _getToken();
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    }));
  }

  final Dio _dio;
  final String? Function() _getToken;
  final void Function() _onUnauthorized;

  // ---- Auth ----

  Future<UserSession> register(String fullName, String email, String password) => _call(
        () => _dio.post('/api/v1/auth/register',
            data: {'fullName': fullName, 'email': email, 'password': password}),
        (data) => UserSession.fromApi(data as Map<String, dynamic>),
      );

  Future<UserSession> login(String email, String password) => _call(
        () => _dio.post('/api/v1/auth/login', data: {'email': email, 'password': password}),
        (data) => UserSession.fromApi(data as Map<String, dynamic>),
      );

  // ---- Dossiers ----

  Future<KycCase> createCase({
    required String fullName,
    required String idNumber,
    required String birthDateIso,
  }) =>
      _call(
        () => _dio.post('/api/v1/kyc/cases', data: {
          'declaredFullName': fullName,
          'declaredIdNumber': idNumber,
          'declaredBirthDate': birthDateIso,
        }),
        _parseCase,
      );

  Future<List<KycCase>> listCases() => _call(
        () => _dio.get('/api/v1/kyc/cases'),
        (data) => (data as List).map((e) => KycCase.fromJson(e as Map<String, dynamic>)).toList(),
      );

  Future<KycCase> getCase(int id) => _call(() => _dio.get('/api/v1/kyc/cases/$id'), _parseCase);

  Future<KycCase> uploadSelfie(int id, XFile file) =>
      _upload('/api/v1/kyc/cases/$id/selfie', file, fallbackMime: 'image/jpeg');

  Future<KycCase> uploadVideo(int id, XFile file) =>
      _upload('/api/v1/kyc/cases/$id/video', file, fallbackMime: 'video/mp4');

  /// [type] : "CIN" ou "PASSPORT".
  Future<KycCase> uploadDocument(int id, String type, XFile file) =>
      _upload('/api/v1/kyc/cases/$id/document', file, fallbackMime: 'image/jpeg', query: {'type': type});

  /// Soumet le dossier : le backend lance la vérification et renvoie le statut final.
  Future<KycCase> submit(int id) => _call(() => _dio.post('/api/v1/kyc/cases/$id/submit'), _parseCase);

  // ---- Utilitaires ----

  static KycCase _parseCase(dynamic data) => KycCase.fromJson(data as Map<String, dynamic>);

  Future<KycCase> _upload(
    String path,
    XFile file, {
    required String fallbackMime,
    Map<String, dynamic>? query,
  }) =>
      _call(() async {
        final form = FormData.fromMap({
          'file': await MultipartFile.fromFile(
            file.path,
            filename: file.name,
            // Le backend n'accepte que JPEG/PNG (images) et MP4/WebM/MOV (vidéo) :
            // le type MIME est déduit de l'extension du fichier.
            contentType: MediaType.parse(_mimeFor(file.name, fallbackMime)),
          ),
        });
        return _dio.post(path, data: form, queryParameters: query);
      }, _parseCase);

  static String _mimeFor(String fileName, String fallback) {
    final dot = fileName.lastIndexOf('.');
    final ext = dot < 0 ? '' : fileName.substring(dot + 1).toLowerCase();
    return switch (ext) {
      'jpg' || 'jpeg' => 'image/jpeg',
      'png' => 'image/png',
      'mp4' => 'video/mp4',
      'webm' => 'video/webm',
      'mov' => 'video/quicktime',
      _ => fallback,
    };
  }

  Future<T> _call<T>(Future<Response<dynamic>> Function() request, T Function(dynamic data) parse) async {
    try {
      final response = await request();
      return parse(response.data);
    } on DioException catch (e) {
      // 401 avec un jeton envoyé = session expirée ou invalide : on déconnecte.
      // (Un 401 au login, sans jeton, n'est qu'un mauvais mot de passe.)
      if (e.response?.statusCode == 401 && _getToken() != null) {
        _onUnauthorized();
      }
      throw _toException(e);
    }
  }

  ApiException _toException(DioException e) {
    final status = e.response?.statusCode;
    final data = e.response?.data;
    if (data is Map) {
      final errors = data['errors'];
      if (errors is Map && errors.isNotEmpty) {
        return ApiException(errors.values.join('\n'), statusCode: status);
      }
      final detail = data['detail'];
      if (detail is String && detail.isNotEmpty) {
        return ApiException(detail, statusCode: status);
      }
    }
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.connectionError:
        return const ApiException('Impossible de joindre le serveur. Vérifie ta connexion et l\'adresse du backend.');
      default:
        return ApiException('Erreur inattendue${status != null ? ' ($status)' : ''}', statusCode: status);
    }
  }
}
