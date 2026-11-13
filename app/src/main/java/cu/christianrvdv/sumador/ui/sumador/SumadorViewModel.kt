package cu.christianrvdv.sumador.ui.sumador

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.christianrvdv.sumador.data.database.Converters
import cu.christianrvdv.sumador.data.database.CustomDenominationDao
import cu.christianrvdv.sumador.data.database.CustomDenominationEntity
import cu.christianrvdv.sumador.data.database.SavedSumDao
import cu.christianrvdv.sumador.data.database.SavedSumEntity
import cu.christianrvdv.sumador.ui.settings.CurrencySymbol
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.sumadorDataStore by preferencesDataStore(name = "sumador_state")

@HiltViewModel
class SumadorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedSumDao: SavedSumDao,
    private val customDenominationDao: CustomDenominationDao
) : ViewModel() {

    private val _currencyFlow = MutableStateFlow(CurrencySymbol.PESO)
    private val _useCoinsFlow = MutableStateFlow(false)

    private val _cantidades = mutableStateMapOf<Int, String>()

    private val _state = MutableStateFlow(SumadorState(cantidades = emptyMap(), total = 0L))
    val state: StateFlow<SumadorState> = _state.asStateFlow()

    private var autoSaveEnabled = true

    val denominations: StateFlow<List<CustomDenominationEntity>> = combine(
        _currencyFlow,
        _useCoinsFlow
    ) { currency, useCoins -> currency to useCoins }
        .flatMapLatest { (currency, useCoins) ->
            customDenominationDao.getByCurrency(currency.name)
                .map { customList ->
                    if (customList.isNotEmpty()) {
                        customList
                    } else {
                        getDefaultDenominationsWithType(currency, useCoins)
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            denominations.collect { denomList ->
                val currentMap = _cantidades.toMap()
                val newMap = mutableMapOf<Int, String>()
                denomList.forEach { entity ->
                    newMap[entity.denomination] = currentMap[entity.denomination] ?: ""
                }
                _cantidades.clear()
                _cantidades.putAll(newMap)
                calcularTotal()
            }
        }
    }

    fun setCurrency(currency: CurrencySymbol) {
        if (_currencyFlow.value != currency) {
            _currencyFlow.value = currency
            cargarCantidadesDesdeDataStore(currency, _useCoinsFlow.value)
        } else {
            cargarCantidadesDesdeDataStore(currency, _useCoinsFlow.value)
        }
    }

    fun setUseCoins(enabled: Boolean) {
        if (_useCoinsFlow.value != enabled) {
            _useCoinsFlow.value = enabled
            cargarCantidadesDesdeDataStore(_currencyFlow.value, enabled)
        }
    }

    fun setAutoSave(enabled: Boolean) {
        autoSaveEnabled = enabled
    }

    fun updateCantidad(denominacion: Int, valor: String) {
        _cantidades[denominacion] = valor
        calcularTotal()
        if (autoSaveEnabled) {
            guardarCantidad(denominacion, valor)
        }
    }

    private fun guardarCantidad(denominacion: Int, valor: String) {
        viewModelScope.launch {
            try {
                context.sumadorDataStore.edit { prefs ->
                    val key = stringPreferencesKey("${_currencyFlow.value.name}_$denominacion")
                    prefs[key] = valor
                }
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error saving amount for $denominacion", e)
            }
        }
    }

    private fun cargarCantidadesDesdeDataStore(currency: CurrencySymbol, useCoins: Boolean) {
        viewModelScope.launch {
            try {
                val prefs = context.sumadorDataStore.data.first()
                val currentDenoms = denominations.value.map { it.denomination }
                currentDenoms.forEach { denom ->
                    val key = stringPreferencesKey("${currency.name}_$denom")
                    val saved = prefs[key] ?: ""
                    _cantidades[denom] = saved
                }
                calcularTotal()
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error loading amounts for $currency", e)
            }
        }
    }

    private fun calcularTotal() {
        val total = _cantidades.entries.sumOf { (denom, value) ->
            val cantidad = value.toIntOrNull() ?: 0
            denom.toLong() * cantidad
        }
        _state.update {
            it.copy(total = total, cantidades = _cantidades.toMap())
        }
    }

    fun resetear() {
        val denoms = denominations.value.map { it.denomination }
        denoms.forEach { _cantidades[it] = "" }
        calcularTotal()
        if (autoSaveEnabled) {
            guardarReset()
        }
    }

    private fun guardarReset() {
        viewModelScope.launch {
            try {
                context.sumadorDataStore.edit { prefs ->
                    val denoms = denominations.value.map { it.denomination }
                    denoms.forEach { denom ->
                        val key = stringPreferencesKey("${_currencyFlow.value.name}_$denom")
                        prefs[key] = ""
                    }
                }
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error resetting state", e)
            }
        }
    }

    fun saveCurrentSum(name: String, total: Long, denominationsMap: Map<Int, Int>) {
        viewModelScope.launch {
            try {
                val entity = SavedSumEntity(
                    name = name,
                    timestamp = System.currentTimeMillis(),
                    total = total,
                    denominationsMap = Converters().fromMapToString(denominationsMap)
                )
                savedSumDao.insert(entity)
            } catch (e: Exception) {
                Log.e("SumadorViewModel", "Error saving sum", e)
            }
        }
    }

    private fun getDefaultDenominationsWithType(currency: CurrencySymbol, useCoins: Boolean): List<CustomDenominationEntity> {
        return when (currency) {
            CurrencySymbol.PESO -> {
                val bills = listOf(100, 300, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000)
                val coins = listOf(1, 2, 5, 20, 50)
                val all = if (useCoins) bills + coins else bills
                all.map { value ->
                    CustomDenominationEntity(
                        currency = currency.name,
                        denomination = value,
                        isCoin = value in coins
                    )
                }.sortedBy { it.denomination }
            }
            CurrencySymbol.USD -> {
                val bills = listOf(100, 200, 500, 1000, 2000, 5000, 10000)
                val coins = listOf(1, 5, 10, 25, 50)
                val all = if (useCoins) bills + coins else bills
                all.map { value ->
                    CustomDenominationEntity(
                        currency = currency.name,
                        denomination = value,
                        isCoin = value in coins
                    )
                }.sortedBy { it.denomination }
            }
            CurrencySymbol.EURO -> {
                val bills = listOf(500, 1000, 2000, 5000, 10000, 20000, 50000)
                val coins = listOf(1, 2, 5, 10, 20, 50)
                val all = if (useCoins) bills + coins else bills
                all.map { value ->
                    CustomDenominationEntity(
                        currency = currency.name,
                        denomination = value,
                        isCoin = value in coins
                    )
                }.sortedBy { it.denomination }
            }
        }
    }
}