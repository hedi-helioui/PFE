String _two(int n) => n.toString().padLeft(2, '0');

/// 14/05/1998
String formatDate(DateTime d) => '${_two(d.day)}/${_two(d.month)}/${d.year}';

/// 14/05/1998 à 09:30
String formatDateTime(DateTime d) =>
    '${formatDate(d)} à ${_two(d.hour)}:${_two(d.minute)}';

/// 1998-05-14, le format attendu par l'API.
String toIsoDate(DateTime d) => '${d.year.toString().padLeft(4, '0')}-${_two(d.month)}-${_two(d.day)}';
