package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextRevision

data class Analysis(val revision: TextRevision, val issues: List<CodeIssue>)