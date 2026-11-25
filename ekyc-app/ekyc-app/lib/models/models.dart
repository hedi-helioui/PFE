/// Statuts d'un dossier (miroir de CaseStatus côté backend).
enum CaseStatus {
  pending,
  processing,
  approved,
  rejected,
  manualReview;

  static CaseStatus parse(String value) => switch (value) {
        'PENDING' => CaseStatus.pending,
        'PROCESSING' => CaseStatus.processing,
        'APPROVED' => CaseStatus.approved,
        'REJECTED' => CaseStatus.rejected,
        'MANUAL_REVIEW' => CaseStatus.manualReview,
        _ => CaseStatus.pending,
      };

  String get label => switch (this) {
        CaseStatus.pending => 'Brouillon',
        CaseStatus.processing => 'Analyse en cours',
        CaseStatus.approved => 'Approuvé',
        CaseStatus.rejected => 'Rejeté',
        CaseStatus.manualReview => 'En revue manuelle',
      };
}

/// Session de l'utilisateur connecté (réponse de /auth/login ou /auth/register).
class UserSession {
  const UserSession({
    required this.token,
    required this.email,
    required this.fullName,
    required this.role,
    required this.expiresAt,
  });

  final String token;
  final String email;
  final String fullName;
  final String role;
  final DateTime expiresAt;

  bool get isExpired => DateTime.now().isAfter(expiresAt);

  /// Réponse de l'API (AuthResponse).
  factory UserSession.fromApi(Map<String, dynamic> json) => UserSession(
        token: json['token'] as String,
        email: json['email'] as String,
        fullName: json['fullName'] as String,
        role: json['role'] as String,
        expiresAt: DateTime.now().add(Duration(seconds: (json['expiresInSeconds'] as num).toInt())),
      );

  /// Format de stockage local.
  factory UserSession.fromJson(Map<String, dynamic> json) => UserSession(
        token: json['token'] as String,
        email: json['email'] as String,
        fullName: json['fullName'] as String,
        role: json['role'] as String,
        expiresAt: DateTime.parse(json['expiresAt'] as String),
      );

  Map<String, dynamic> toJson() => {
        'token': token,
        'email': email,
        'fullName': fullName,
        'role': role,
        'expiresAt': expiresAt.toIso8601String(),
      };
}

class DocumentInfo {
  const DocumentInfo({
    required this.id,
    required this.type,
    this.ocrFullName,
    this.ocrIdNumber,
    this.ocrBirthDate,
  });

  final int id;
  final String type; // CIN | PASSPORT
  final String? ocrFullName;
  final String? ocrIdNumber;
  final String? ocrBirthDate;

  factory DocumentInfo.fromJson(Map<String, dynamic> json) => DocumentInfo(
        id: (json['id'] as num).toInt(),
        type: json['type'] as String,
        ocrFullName: json['ocrFullName'] as String?,
        ocrIdNumber: json['ocrIdNumber'] as String?,
        ocrBirthDate: json['ocrBirthDate'] as String?,
      );
}

class MediaInfo {
  const MediaInfo({required this.id, required this.kind});

  final int id;
  final String kind; // SELFIE | VIDEO

  factory MediaInfo.fromJson(Map<String, dynamic> json) =>
      MediaInfo(id: (json['id'] as num).toInt(), kind: json['kind'] as String);
}

class VerificationResult {
  const VerificationResult({
    required this.faceMatchScore,
    required this.livenessScore,
    required this.fraudScore,
    required this.consistencyScore,
    required this.outcome,
  });

  final double faceMatchScore;
  final double livenessScore;
  final double fraudScore;
  final double consistencyScore;
  final CaseStatus outcome;

  factory VerificationResult.fromJson(Map<String, dynamic> json) => VerificationResult(
        faceMatchScore: (json['faceMatchScore'] as num).toDouble(),
        livenessScore: (json['livenessScore'] as num).toDouble(),
        fraudScore: (json['fraudScore'] as num).toDouble(),
        consistencyScore: (json['consistencyScore'] as num).toDouble(),
        outcome: CaseStatus.parse(json['outcome'] as String),
      );
}

class KycCase {
  const KycCase({
    required this.id,
    required this.status,
    required this.declaredFullName,
    required this.declaredIdNumber,
    required this.declaredBirthDate,
    required this.createdAt,
    required this.documents,
    required this.media,
    this.decisionComment,
    this.result,
  });

  final int id;
  final CaseStatus status;
  final String declaredFullName;
  final String declaredIdNumber;
  final DateTime declaredBirthDate;
  final DateTime createdAt;
  final String? decisionComment;
  final List<DocumentInfo> documents;
  final List<MediaInfo> media;
  final VerificationResult? result;

  bool get hasSelfie => media.any((m) => m.kind == 'SELFIE');
  bool get hasVideo => media.any((m) => m.kind == 'VIDEO');
  bool get hasDocument => documents.isNotEmpty;
  bool get isComplete => hasSelfie && hasVideo && hasDocument;

  factory KycCase.fromJson(Map<String, dynamic> json) => KycCase(
        id: (json['id'] as num).toInt(),
        status: CaseStatus.parse(json['status'] as String),
        declaredFullName: json['declaredFullName'] as String,
        declaredIdNumber: json['declaredIdNumber'] as String,
        declaredBirthDate: DateTime.parse(json['declaredBirthDate'] as String),
        createdAt: DateTime.parse(json['createdAt'] as String).toLocal(),
        decisionComment: json['decisionComment'] as String?,
        documents: ((json['documents'] as List?) ?? const [])
            .map((e) => DocumentInfo.fromJson(e as Map<String, dynamic>))
            .toList(),
        media: ((json['media'] as List?) ?? const [])
            .map((e) => MediaInfo.fromJson(e as Map<String, dynamic>))
            .toList(),
        result: json['result'] == null
            ? null
            : VerificationResult.fromJson(json['result'] as Map<String, dynamic>),
      );
}
