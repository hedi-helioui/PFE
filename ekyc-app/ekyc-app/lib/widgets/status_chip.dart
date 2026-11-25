import 'package:flutter/material.dart';

import '../models/models.dart';

class StatusChip extends StatelessWidget {
  const StatusChip(this.status, {super.key});

  final CaseStatus status;

  MaterialColor get _color => switch (status) {
        CaseStatus.approved => Colors.green,
        CaseStatus.rejected => Colors.red,
        CaseStatus.manualReview => Colors.orange,
        CaseStatus.processing => Colors.blue,
        CaseStatus.pending => Colors.grey,
      };

  @override
  Widget build(BuildContext context) {
    final color = _color;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.shade100,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Text(
        status.label,
        style: TextStyle(color: color.shade900, fontWeight: FontWeight.w600, fontSize: 12),
      ),
    );
  }
}
