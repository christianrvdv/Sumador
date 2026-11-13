package cu.christianrvdv.sumador.ui.sumador

data class SumadorState(
    val cantidades: Map<Int, String> = emptyMap(),
    val total: Long = 0L
)