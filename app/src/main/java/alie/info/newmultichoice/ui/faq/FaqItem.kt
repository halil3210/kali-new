package alie.info.newmultichoice.ui.faq

data class FaqItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)

