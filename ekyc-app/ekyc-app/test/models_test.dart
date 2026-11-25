import 'package:ekyc_app/models/models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const json = {
    'id': 7,
    'status': 'MANUAL_REVIEW',
    'clientEmail': 'client@test.com',
    'declaredFullName': 'Mohamed Ben Ali',
    'declaredIdNumber': '12345678',
    'declaredBirthDate': '1998-05-14',
    'createdAt': '2026-10-28T10:15:30Z',
    'updatedAt': '2026-10-28T10:16:00Z',
    'decisionComment': null,
    'decidedBy': null,
    'documents': [
      {'id': 1, 'type': 'CIN', 'ocrFullName': 'Mohamed Ben Ali', 'ocrIdNumber': '12345678', 'ocrBirthDate': '1998-05-14'}
    ],
    'media': [
      {'id': 1, 'kind': 'SELFIE'},
      {'id': 2, 'kind': 'VIDEO'},
    ],
    'result': {
      'faceMatchScore': 0.65,
      'livenessScore': 0.95,
      'fraudScore': 0.05,
      'consistencyScore': 1.0,
      'outcome': 'MANUAL_REVIEW',
    },
  };

  test('parse une réponse CaseResponse du backend', () {
    final c = KycCase.fromJson(json);
    expect(c.id, 7);
    expect(c.status, CaseStatus.manualReview);
    expect(c.declaredBirthDate, DateTime(1998, 5, 14));
    expect(c.hasSelfie && c.hasVideo && c.hasDocument, isTrue);
    expect(c.isComplete, isTrue);
    expect(c.result!.faceMatchScore, 0.65);
    expect(c.result!.outcome, CaseStatus.manualReview);
  });

  test('un dossier vide n\'est pas complet et n\'a pas de résultat', () {
    final c = KycCase.fromJson({
      ...json,
      'status': 'PENDING',
      'documents': <dynamic>[],
      'media': <dynamic>[],
      'result': null,
    });
    expect(c.status, CaseStatus.pending);
    expect(c.isComplete, isFalse);
    expect(c.result, isNull);
  });

  test('statuts inconnus retombent sur brouillon', () {
    expect(CaseStatus.parse('???'), CaseStatus.pending);
    expect(CaseStatus.parse('APPROVED'), CaseStatus.approved);
  });

  test('session : stockage local aller-retour et expiration', () {
    final session = UserSession(
      token: 't',
      email: 'a@b.c',
      fullName: 'A B',
      role: 'CLIENT',
      expiresAt: DateTime.now().subtract(const Duration(minutes: 1)),
    );
    final restored = UserSession.fromJson(session.toJson());
    expect(restored.email, 'a@b.c');
    expect(restored.isExpired, isTrue);
  });
}
