package com.rentalvalidator.app.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.NavDestination
import com.rentalvalidator.app.data.local.AppDatabase
import com.rentalvalidator.app.data.local.datastore.PreferencesManager
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.data.repository.*
import com.rentalvalidator.app.domain.model.*
import com.rentalvalidator.app.domain.usecase.ValidatePaymentsUseCase
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.theme.RentalValidatorTheme
import com.rentalvalidator.app.presentation.ui.dashboard.DashboardScreen
import com.rentalvalidator.app.presentation.ui.tenants.TenantsScreen
import com.rentalvalidator.app.presentation.ui.contracts.ContractsScreen
import com.rentalvalidator.app.presentation.ui.monthly_grid.*
import com.rentalvalidator.app.presentation.ui.validator.ValidatorScreen
import com.rentalvalidator.app.presentation.ui.settings.SettingsScreen
import com.rentalvalidator.app.presentation.viewmodel.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.viewModelScope
import org.junit.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Exercises production screens with real repositories and an isolated in-memory Room database.
 * No user records, backup formats, or application preferences are written by this test.
 */
class VisualFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var tenants: TenantsViewModel
    private lateinit var units: UnitsViewModel
    private lateinit var contracts: ContractsViewModel
    private lateinit var grid: MonthlyGridViewModel
    private lateinit var validator: ValidatorViewModel
    private lateinit var settings: SettingsViewModel
    private lateinit var reminderVm: RentReminderViewModel
    private lateinit var reminderScheduler: com.rentalvalidator.app.reminders.RentReminderScheduler
    private lateinit var reminderPrefsName: String
    private val route = mutableStateOf("dashboard")
    private val scopedTenant = mutableStateOf<String?>(null)
    private val dark = mutableStateOf(false)
    private val fontScale = mutableFloatStateOf(1f)

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val tr = TenantRepositoryImpl(db.tenantDao())
        val pr = PaymentRepositoryImpl(db.paymentDao())
        val ur = UnitRepositoryImpl(db.unitDao())
        val unit = RentalUnit("visual-unit", "Jardim das Oliveiras", UnitType.APARTMENT,
            location = "Rua das Oliveiras, 84", capacity = 4)
        ur.insertUnit(unit)
        listOf("Marina Oliveira", "Rafael Mendes", "Ana Beatriz de Albuquerque").forEachIndexed { i, name ->
            tr.insertTenant(Tenant("visual-$i", name, 1250.0 + i * 300, 10 + i * 5,
                bank = "Nubank", phone = "11987654321", unit = unit.name, unitId = unit.id,
                contractExpirationDate = LocalDate.now().plusDays(20).toString()))
        }
        val now = LocalDate.now()
        pr.savePayment("visual-0", now.year, now.monthValue, PaymentStatus.PAGO)
        (1..3).forEach { offset ->
            val date = now.minusMonths(offset.toLong())
            pr.savePayment("visual-0", date.year, date.monthValue, PaymentStatus.PAGO)
        }
        val prefs = PreferencesManager(context)
        tenants = TenantsViewModel(tr, pr)
        units = UnitsViewModel(ur)
        contracts = ContractsViewModel(tr, com.rentalvalidator.app.data.files.DocumentStorage(context))
        grid = MonthlyGridViewModel(tr, pr)
        validator = ValidatorViewModel(ValidatePaymentsUseCase(), tr, pr, prefs, com.rentalvalidator.app.data.importer.StatementReader(context))
        reminderPrefsName = "visual-flow-reminder-" + java.util.UUID.randomUUID()
        val isolatedReminders = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) =
                context.getSharedPreferences(reminderPrefsName, mode)
        }
        reminderScheduler = com.rentalvalidator.app.reminders.RentReminderScheduler(isolatedReminders, db.tenantDao())
        settings = SettingsViewModel(prefs, com.rentalvalidator.app.data.backup.LegacyBackupService(context, db.backupDao()),
            com.rentalvalidator.app.data.backup.CompleteBackupService(
                context, db.backupDao(), prefs, reminderScheduler))
        reminderVm = RentReminderViewModel(reminderScheduler)
    }

    @After fun close() {
        val scopes = listOf(tenants, units, contracts, grid, validator, settings, reminderVm).map { it.viewModelScope }
        compose.runOnIdle { scopes.forEach { it.cancel() } }
        runBlocking { scopes.forEach { it.coroutineContext[Job]?.join() } }
        runBlocking { listOf("visual-0", "visual-1", "visual-2").forEach { reminderScheduler.remove(it) } }
        context.getSharedPreferences(reminderPrefsName, Context.MODE_PRIVATE).edit().clear().commit()
        db.close()
    }

    private fun launch() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                RentalValidatorTheme(darkTheme = dark.value) {
                    val snackbars = remember { SnackbarHostState() }
                    AppSnackbarProvider(snackbars) {
                        Scaffold(contentWindowInsets = WindowInsets.statusBars,
                            snackbarHost = { AppSnackbarHost(snackbars) },
                            bottomBar = { if (route.value != "settings") AppNavigation(
                                NavDestination("fixture").apply { this.route = this@VisualFlowTest.route.value },
                                { route.value = it.route }) }
                        ) { padding ->
                            Column(Modifier.fillMaxSize().padding(padding)) {
                              Box(Modifier.weight(1f)) {
                                when (route.value) {
                                    "dashboard" -> DashboardScreen(tenants, units, { route.value = "settings" }, { route.value = "contracts" },{route.value="grid"},{route.value="validator"},{route.value="tenants"},
                                        onNavigateToTenantPayments = { scopedTenant.value = it; route.value = "tenant-payments" })
                                    "tenants" -> TenantsScreen(tenants, units, {}, {}, { route.value = "grid" },
                                        onNavigateToTenantPayments = { scopedTenant.value=it;route.value="tenant-payments" },
                                        reminderViewModel = reminderVm)
                                    "tenant-payments" -> MonthlyGridScreen(grid,tenantId=scopedTenant.value,onBack={route.value="tenants"})
                                    "grid" -> MonthlyGridScreen(grid){route.value=it.route}
                                    "contracts" -> ContractsScreen(contracts, units)
                                    "validator" -> ValidatorScreen(validator){route.value=it.route}
                                    "settings" -> SettingsScreen(settings) { route.value = "dashboard" }
                                }
                              }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(450) // Android window animations run outside the Compose test clock.
        compose.waitForIdle()
        val folderName = InstrumentationRegistry.getArguments().getString("reviewFolder") ?: "nani-rebuild-review"
        val folder = File(context.getExternalFilesDir(null), folderName).apply { mkdirs() }
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun select(next: String) { compose.runOnIdle { route.value = next }; compose.waitForIdle() }

    @Test fun screensInLightAndDark() {
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        listOf(false, true).forEach { isDark ->
            compose.runOnIdle { dark.value = isDark }
            val theme = if (isDark) "dark" else "light"
            listOf("dashboard", "tenants", "grid", "contracts", "validator", "settings").forEach {
                select(it)
                if (it == "contracts") compose.waitUntil(10_000) { contracts.tenants.value.size == 3 }
                shot("$theme-$it")
            }
        }
        compose.runOnIdle { fontScale.floatValue = 1.3f }
        select("dashboard")
        shot("large-type-dashboard")
        select("settings")
        shot("large-type-settings")
        compose.runOnIdle { fontScale.floatValue = 1.6f }
        select("contracts")
        shot("large-type-contracts")
        select("grid")
        shot("large-type-grid")
    }

    @Test fun dashboardShowsSummaryAndHistoryAndKeepsTotalsReachable() {
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNodeWithText("Acompanhar recebimentos").assertIsDisplayed()
        compose.onNode(hasText("recebimentos pendentes", substring = true)).assertDoesNotExist()
        compose.onNodeWithText("Histórico de recebimentos").assertIsDisplayed()
        // The summary and history heading are visible first; smaller screens scroll to the footer.
        compose.onNode(hasText("Total no período", substring = true)).performScrollTo().assertIsDisplayed()
        shot("dashboard-compact-overview")
    }

    /** The two leading facts of a detail page share one line even when a value is wide. */
    @Test fun heroFactsStaySideBySideWithWideValues() {
        compose.setContent {
            RentalValidatorTheme(darkTheme = true) {
                Surface {
                    Column(Modifier.width(320.dp).statusBarsPadding()) {
                        com.rentalvalidator.app.presentation.design.NaniDetailHero(
                            "Esio 46", "R. Macieira N10 - Guarulhos", "Aluguéis por mês", "R$ 36.555,00", "Inquilinos", "5 de 4",
                            identity = com.rentalvalidator.app.presentation.design.DetailIdentity.UNIT,
                            unitIcon = androidx.compose.material.icons.Icons.Rounded.Home)
                        // A long name and a long unit wrap inside the cover without pushing the facts apart.
                        com.rentalvalidator.app.presentation.design.NaniDetailHero(
                            "Ana Beatriz de Albuquerque Figueiredo", "Residencial Jardim das Oliveiras, bloco B, apartamento 302",
                            "Aluguel mensal", "R$ 12.480,00", "Vencimento", "Dia 28")
                    }
                }
            }
        }
        val amount = compose.onNodeWithText("R$ 36.555,00").fetchSemanticsNode()
        val count = compose.onNodeWithText("5 de 4").fetchSemanticsNode()
        Assert.assertEquals("Values share one line", amount.boundsInRoot.top, count.boundsInRoot.top, 1.5f)
        Assert.assertTrue("Values sit side by side", amount.boundsInRoot.right < count.boundsInRoot.left)
        listOf("R$ 36.555,00", "5 de 4").forEach { value ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(value).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            Assert.assertEquals(1, layouts.single().lineCount)
            Assert.assertTrue("$value must fit its half", layouts.single().getLineRight(0) <= layouts.single().size.width + 1f)
        }
        val name = compose.onNodeWithText("Ana Beatriz de Albuquerque Figueiredo").fetchSemanticsNode()
        val rent = compose.onNodeWithText("R$ 12.480,00").fetchSemanticsNode()
        val due = compose.onNodeWithText("Dia 28").fetchSemanticsNode()
        Assert.assertTrue("The facts sit below the cover", rent.boundsInRoot.top > name.boundsInRoot.bottom)
        Assert.assertEquals("Long names leave the facts level", rent.boundsInRoot.top, due.boundsInRoot.top, 1.5f)
        shot("hero-wide-values")
    }

    /** Saves the frame the paused test clock has reached, without waiting for animations to settle. */
    private fun frame(name: String) {
        Thread.sleep(250) // Lets the window draw the frame produced by the last clock advance.
        val folderName = InstrumentationRegistry.getArguments().getString("reviewFolder") ?: "nani-rebuild-review"
        val folder = File(context.getExternalFilesDir(null), folderName).apply { mkdirs() }
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    /**
     * A unit card shows only its name and address on top and at most four portraits below. Opening
     * it turns the portraits into the residents' lines; closing it folds them back into the pile.
     */
    @Test fun unitCardUnfoldsResidentsFromThePile() {
        val home = RentalUnit("fold-unit", "Vila das Acácias", UnitType.HOUSE, location = "Rua das Acácias, 12",
            capacity = 8, tenantCount = 6)
        val people = listOf("Clara Nunes", "Davi Rocha", "Elisa Prado", "Felipe Antunes", "Gabriela Lins", "Heitor Campos")
            .mapIndexed { i, name -> Tenant("fold-$i", name, 1200.0 + i * 50, 5 + i, unit = home.name, unitId = home.id) }
        compose.setContent {
            RentalValidatorTheme(darkTheme = dark.value) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.statusBarsPadding()) {
                        com.rentalvalidator.app.presentation.ui.tenants.TenantsOverview(people, listOf(home),
                            com.rentalvalidator.app.presentation.ui.tenants.TenantViewMode.UNITS, "", false,
                            onQuery = {}, onMode = {}, onUnit = {}, onTenant = {}, onFilters = {}, onAdd = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Vila das Acácias").assertIsDisplayed()
        compose.onNodeWithText("Rua das Acácias, 12").assertIsDisplayed()
        listOf("Casa", "Ocupação parcial", "Ocupada").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNode(hasContentDescription("Clara Nunes, Davi Rocha, Elisa Prado, Felipe Antunes e mais 2")).assertExists()
        compose.onNodeWithText("Gabriela Lins").assertDoesNotExist()
        shot("unit-fold-closed")

        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Mostrar inquilinos").performClick()
        listOf(70L, 90L, 110L, 160L).fold(0L) { elapsed, step ->
            compose.mainClock.advanceTimeBy(step)
            (elapsed + step).also { frame("unit-fold-opening-$it") }
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        people.forEach { compose.onNodeWithText(it.name).assertExists() }
        compose.onNode(hasContentDescription("e mais 2", substring = true)).assertDoesNotExist()
        shot("unit-fold-open")

        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Recolher inquilinos").performClick()
        listOf(70L, 90L, 110L, 160L).fold(0L) { elapsed, step ->
            compose.mainClock.advanceTimeBy(step)
            (elapsed + step).also { frame("unit-fold-closing-$it") }
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNode(hasContentDescription("e mais 2", substring = true)).assertExists()
        compose.onNodeWithText("Gabriela Lins").assertDoesNotExist()
        compose.runOnIdle { dark.value = true }
        shot("unit-fold-closed-dark")
    }

    /**
     * On a narrow phone a full unit's pile, the toggle label and the chevron share one row. The label
     * and the chevron hold their places while the pile leaves and returns, and the pile never covers them.
     */
    @Test fun unitToggleHoldsStillOnNarrowScreens() {
        val home = RentalUnit("narrow-unit", "Vila das Acácias", UnitType.HOUSE, capacity = 8, tenantCount = 6)
        val people = listOf("Clara Nunes", "Davi Rocha", "Elisa Prado", "Felipe Antunes", "Gabriela Lins", "Heitor Campos")
            .mapIndexed { i, name -> Tenant("narrow-$i", name, 1200.0, 5 + i, unit = home.name, unitId = home.id) }
        compose.setContent {
            RentalValidatorTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // A Surface passes its minimum size on to its child, so the narrow column sits in a Box of its own.
                    Box {
                        Box(Modifier.statusBarsPadding().width(360.dp)) {
                            com.rentalvalidator.app.presentation.ui.tenants.TenantsOverview(people, listOf(home),
                                com.rentalvalidator.app.presentation.ui.tenants.TenantViewMode.UNITS, "", false,
                                onQuery = {}, onMode = {}, onUnit = {}, onTenant = {}, onFilters = {}, onAdd = {})
                        }
                    }
                }
            }
        }
        fun chevron(description: String) = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        fun label(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val resting = label("Ver inquilinos")
        val chevronAtRest = chevron("Mostrar inquilinos")
        val pile = compose.onNode(hasContentDescription("e mais", substring = true), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        Assert.assertEquals("The chevron keeps its full size", chevronAtRest.height, chevronAtRest.width, 1.5f)
        Assert.assertTrue("The pile stays clear of the label", pile.right <= resting.left)

        fun assertHeldStill(text: String, description: String) {
            val shown = label(text)
            Assert.assertEquals("$text keeps the label's right edge", resting.right, shown.right, 1.5f)
            Assert.assertEquals("$text stays on one line", resting.height, shown.height, 1.5f)
            val now = chevron(description)
            Assert.assertEquals("The chevron does not move", chevronAtRest.left, now.left, 1.5f)
            Assert.assertEquals("The chevron is not squeezed", chevronAtRest.width, now.width, 1.5f)
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Mostrar inquilinos").performClick()
        repeat(16) {
            compose.mainClock.advanceTimeBy(40)
            assertHeldStill("Ocultar inquilinos", "Recolher inquilinos")
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        people.forEach { compose.onNodeWithText(it.name).assertExists() }
        assertHeldStill("Ocultar inquilinos", "Recolher inquilinos")

        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Recolher inquilinos").performClick()
        repeat(16) {
            compose.mainClock.advanceTimeBy(40)
            assertHeldStill("Ver inquilinos", "Mostrar inquilinos")
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertHeldStill("Ver inquilinos", "Mostrar inquilinos")
    }

    /** The production shell, not this harness's Scaffold, must give page text the theme's ink. */
    @Test fun appShellInksPageTextForTheNightTheme() {
        var ink = androidx.compose.ui.graphics.Color.Unspecified
        var expected = androidx.compose.ui.graphics.Color.Unspecified
        compose.setContent {
            RentalValidatorTheme(darkTheme = true) {
                com.rentalvalidator.app.presentation.navigation.NaniScaffold(null, {}) {
                    ink = LocalContentColor.current
                    expected = MaterialTheme.colorScheme.onBackground
                    Text("Histórico de recebimentos")
                }
            }
        }
        compose.waitForIdle()
        Assert.assertEquals(expected, ink)
        Assert.assertNotEquals(androidx.compose.ui.graphics.Color.Black, ink)
    }

    @Test fun dashboardListsOpenRentsAndOpensTheTenantsPayments() {
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 && tenants.recentPayments.value.isNotEmpty() }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Quem falta pagar"))
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Rafael Mendes"))
        // Marina paid this month, so only the two open rents are listed.
        compose.onNode(hasText("Rafael Mendes") and hasClickAction()).assertIsDisplayed()
        compose.onNode(hasText("Ana Beatriz de Albuquerque") and hasClickAction()).assertExists()
        compose.onNode(hasText("Marina Oliveira") and hasClickAction()).assertDoesNotExist()
        shot("dashboard-open-rents")
        compose.onNode(hasText("Rafael Mendes") and hasClickAction()).performClick()
        compose.waitUntil(10_000) { route.value == "tenant-payments" && grid.state.value.items.size == 3 }
        Assert.assertEquals("visual-1", scopedTenant.value)
        compose.onNodeWithText("Ana Beatriz de Albuquerque").assertDoesNotExist()
        compose.onNodeWithText("Pagamentos em ${java.time.LocalDate.now().year}").assertIsDisplayed()
        shot("dashboard-open-rent-scoped-payments")
    }

    @Test fun tenantPaymentsStayScopedAcrossMonthsAndStatusChanges() {
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNodeWithText("Inquilinos").performClick()
        compose.onNodeWithText("Marina Oliveira").performClick()
        compose.onNodeWithText("Pagamentos").performClick()
        compose.waitUntil(10_000) { route.value == "tenant-payments" && grid.state.value.items.size == 3 }
        compose.onNodeWithText("Rafael Mendes").assertDoesNotExist()
        compose.onNodeWithText("Ana Beatriz de Albuquerque").assertDoesNotExist()
        shot("tenant-payments-scoped")
        compose.onNodeWithContentDescription("Mês anterior").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Rafael Mendes").assertDoesNotExist()
        compose.onAllNodesWithText("Marina Oliveira").onLast().performClick()
        compose.onNodeWithText("Em análise").performClick()
        compose.waitUntil(10_000) { grid.state.value.items.single { it.tenant.id=="visual-0" }.payment?.status==PaymentStatus.EM_ANALISE }
        Assert.assertNull(grid.state.value.items.single { it.tenant.id=="visual-1" }.payment)
    }

    @Test fun tenantContactOffersWhatsAppAndDialer() {
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNodeWithText("Inquilinos").performClick()
        compose.onNodeWithText("Marina Oliveira").performClick()
        compose.onNodeWithText("Contato").performClick()
        compose.onNodeWithText("WhatsApp").assertIsDisplayed()
        compose.onNodeWithText("Ligar").assertIsDisplayed()
        shot("tenant-contact-options")
    }

    @Test fun tenantReminderShowsSavedStateAndClearsAfterRemoval() {
        runBlocking { reminderScheduler.save("visual-0", 48) }
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNodeWithText("Inquilinos").performClick()
        compose.onNodeWithText("Marina Oliveira").performClick()
        compose.waitUntil(10_000) { !reminderVm.state.value.loading && reminderVm.state.value.reminder != null }
        compose.onNodeWithText("Agendado").assertIsDisplayed()
        compose.onNodeWithText("2 dias antes do vencimento").assertIsDisplayed()
        shot("tenant-reminder-active")
        compose.onNodeWithText("Lembrete").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Remover lembrete").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Remover lembrete").performClick()
        compose.waitUntil(10_000) { reminderVm.state.value.reminder == null }
        compose.onNodeWithText("Agendado").assertDoesNotExist()
        Assert.assertNull(runBlocking { reminderScheduler.get("visual-0") })
    }
    @Test fun reminderEditorOffersPresetsAndValidatesCustomHours() {
        var saved: Int? = null
        val tenant = Tenant("editor-only", "Marina Oliveira", 1250.0, 10)
        compose.setContent {
            RentalValidatorTheme(darkTheme=true) {
                com.rentalvalidator.app.presentation.ui.tenants.ReminderEditor(
                    tenant, ReminderUiState(loading=false), true, false, {}, {}, {saved=it}, {})
            }
        }
        compose.onNodeWithText("2 dias antes").performClick()
        compose.onNodeWithText("Salvar lembrete").performClick()
        Assert.assertEquals(48,saved)
        shot("reminder-presets-dark")
        compose.onNodeWithText("Personalizar").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("0")
        compose.onNodeWithText("Salvar lembrete").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("6")
        compose.onNodeWithText("Salvar lembrete").performClick()
        Assert.assertEquals(6,saved)
        shot("reminder-custom-hours")
    }

    @Test fun reminderSheetSavesReopensAndRemovesSchedule() {
        val preferencesName = "reminder-ui-${java.util.UUID.randomUUID()}"
        val isolated = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = context.getSharedPreferences(preferencesName, mode)
        }
        val scheduler = com.rentalvalidator.app.reminders.RentReminderScheduler(isolated, db.tenantDao())
        val vm = RentReminderViewModel(scheduler)
        val tenant = runBlocking { db.tenantDao().getTenantById("visual-0")!!.toDomain() }
        val opened = mutableStateOf(true)
        try {
            compose.setContent {
                RentalValidatorTheme(darkTheme=false) {
                    AppSnackbarProvider(remember { SnackbarHostState() }) {
                        if (opened.value) com.rentalvalidator.app.presentation.ui.tenants.TenantReminderSheet(tenant, { opened.value=false }, vm)
                    }
                }
            }
            compose.waitUntil(10_000) { !vm.state.value.loading }
            compose.onNodeWithText("3 dias antes").performClick()
            compose.onNodeWithText("Salvar lembrete").performClick()
            compose.waitUntil(10_000) { !opened.value }
            Assert.assertEquals(72, runBlocking { scheduler.get(tenant.id) }!!.leadHours)
            compose.runOnIdle { opened.value=true }
            compose.waitForIdle()
            compose.waitUntil(10_000) { !vm.state.value.loading && vm.state.value.reminder != null }
            compose.onNodeWithText("Remover lembrete").assertIsDisplayed()
            shot("reminder-saved-light")
            compose.onNodeWithText("Remover lembrete").performClick()
            compose.waitUntil(10_000) { !opened.value }
            Assert.assertNull(runBlocking { scheduler.get(tenant.id) })
        } finally {
            compose.runOnIdle { vm.viewModelScope.cancel() }
            runBlocking { scheduler.remove(tenant.id) }
            isolated.getSharedPreferences("rent_reminders", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun receiptHistoryRespondsToTouchAndDrag() {
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        val chart = compose.onNodeWithTag("receipt-history-chart")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("receipt-history-chart"))
        val initial = chart.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
        chart.performTouchInput { swipeRight(startX = width * .1f, endX = width * .8f) }
        chart.performTouchInput { swipeLeft(startX = width * .9f, endX = width * .1f) }
        compose.waitForIdle()
        val previous = chart.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription]
        Assert.assertNotEquals(initial, previous)
        shot("history-selected-month")
    }

    @Test fun overviewToStatementAndMonthlyNavigation() {
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Conferir um extrato"))
        compose.onNodeWithText("Conferir um extrato").performClick()
        compose.onNodeWithText("Selecionar extrato").assertIsDisplayed()
        compose.onNodeWithText("Mensal").performClick()
        compose.onNodeWithContentDescription("Mês anterior").assertIsDisplayed()
        compose.onAllNodesWithText("Pendentes").filterToOne(hasClickAction()).performClick()
        compose.onNodeWithText("Marina Oliveira").assertDoesNotExist()
        compose.onNodeWithText("Rafael Mendes").assertIsDisplayed()
        shot("light-monthly-pending")
    }

    @Test fun compactHeroesKeepCurrencyOnOneLine() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                RentalValidatorTheme(darkTheme = dark.value) {
                    Surface {
                        Column(Modifier.width(320.dp).statusBarsPadding()) {
                            com.rentalvalidator.app.presentation.design.NaniDetailHero(
                                "Morador Exemplo", "Unidade Exemplo", "Aluguel mensal", "R$ 5.000,00", "Vencimento", "Dia 21"
                            )
                            com.rentalvalidator.app.presentation.design.NaniDetailHero(
                                "Jardim das Oliveiras", "Rua das Oliveiras, 84", "Inquilinos", "3",
                                "Capacidade", "4 inquilinos", identity = com.rentalvalidator.app.presentation.design.DetailIdentity.UNIT
                            )
                        }
                    }
                }
            }
        }
        for (scale in listOf(1f, 1.3f)) {
            compose.runOnIdle { fontScale.floatValue = scale; dark.value = scale > 1f }
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText("R$ 5.000,00").performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
            ) { it(layouts) }
            Assert.assertEquals(1, layouts.single().lineCount)
            shot("compact-heroes-$scale")
            Assert.assertTrue("The full amount must fit at font scale $scale",
                layouts.single().getLineRight(0) <= layouts.single().size.width + 1f)
        }
    }

    @Test fun tenantDetailsFiltersAndForms() {
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNodeWithText("Inquilinos").performClick()
        compose.onNodeWithText("Marina Oliveira").assertIsDisplayed()
        shot("light-tenant-list")
        compose.runOnIdle { dark.value = true }
        shot("dark-tenant-list")
        compose.runOnIdle { fontScale.floatValue = 1.3f }
        shot("large-type-tenant-list")
        compose.runOnIdle { dark.value = false; fontScale.floatValue = 1f }
        compose.onNodeWithText("Marina Oliveira").performClick()
        compose.onNodeWithContentDescription("Editar inquilino").assertIsDisplayed()
        shot("light-tenant-detail")
        compose.runOnIdle { dark.value = true }
        shot("dark-tenant-detail")
        compose.onNodeWithContentDescription("Excluir inquilino").assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Vistoria"))
        shot("dark-tenant-contract-footer")
        compose.onNode(hasScrollToNodeAction()).performScrollToIndex(0)
        compose.runOnIdle { fontScale.floatValue = 1.3f }
        shot("large-type-tenant-detail")
        compose.runOnIdle { dark.value = false; fontScale.floatValue = 1f }
        compose.onNodeWithContentDescription("Editar inquilino").performClick()
        compose.onNodeWithText("Salvar").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
        shot("light-tenant-edit")
        compose.onNodeWithContentDescription("Novo apelido").performScrollTo()
        compose.onNodeWithContentDescription("Novo apelido").assertIsDisplayed()
        compose.onNodeWithContentDescription("Adicionar apelido").assertIsDisplayed().assertIsNotEnabled()
        val aliasFieldBottom = compose.onNodeWithContentDescription("Novo apelido")
            .fetchSemanticsNode().boundsInRoot.bottom
        val addAliasButtonBottom = compose.onNodeWithContentDescription("Adicionar apelido")
            .fetchSemanticsNode().boundsInRoot.bottom
        Assert.assertEquals(aliasFieldBottom, addAliasButtonBottom, 1.5f)
        shot("light-tenant-aliases")
        compose.onNodeWithContentDescription("Novo apelido").performTextInput("Marina Pix")
        compose.onNodeWithContentDescription("Adicionar apelido").assertIsEnabled().performClick()
        compose.onNodeWithText("Marina Pix").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { dark.value = true; fontScale.floatValue = 1.3f }
        shot("dark-large-type-tenant-aliases")
        compose.runOnIdle { dark.value = false; fontScale.floatValue = 1f }
        compose.onNodeWithContentDescription("Nome completo").performScrollTo()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Marina Oliveira Silva")
        shot("light-tenant-keyboard")
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(10_000) { tenants.tenants.value.any { it.name == "Marina Oliveira Silva" } }
        val saved = tenants.tenants.value.single { it.id == "visual-0" }
        Assert.assertEquals(1250.0, saved.amount, 0.0)
        Assert.assertEquals("visual-unit", saved.unitId)
        Assert.assertEquals(10, saved.dueDay)
        shot("light-tenant-saved")
    }

    @Test fun paymentStatusSheetAndValidationResults() {
        route.value = "grid"
        launch()
        compose.waitUntil(10_000) { grid.state.value.items.size == 3 }
        compose.onNodeWithContentDescription("Buscar inquilino").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("Marina")
        shot("monthly-search-keyboard")
        compose.onNodeWithText("Ver resultados").performClick()
        compose.onNodeWithText("Rafael Mendes").assertDoesNotExist()
        compose.onNodeWithContentDescription("Buscar inquilino").performClick()
        compose.onNodeWithContentDescription("Limpar busca").performClick()
        compose.onNodeWithText("Ver resultados").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Rafael Mendes"))
        compose.onNodeWithContentDescription("Mês anterior").assertIsDisplayed()
        compose.onNodeWithText("Todos").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filtrar recebimentos").performClick()
        shot("monthly-search-filter")
        compose.onAllNodesWithText("Pendentes").onLast().performClick()
        compose.onNodeWithText("Marina Oliveira").assertDoesNotExist()
        compose.onNodeWithText("Todos").performClick()
        compose.onNodeWithText("Marina Oliveira").performClick()
        shot("light-payment-sheet")
        compose.onNodeWithText("Em análise").performClick()
        compose.waitUntil(10_000) { grid.state.value.items.any { it.tenant.id == "visual-0" && it.payment?.status == PaymentStatus.EM_ANALISE } }
        compose.onNodeWithContentDescription("Mês anterior").performClick()
        compose.onNodeWithContentDescription("Próximo mês").performClick()
    }

    @Test fun csvValidationAndExpansion() {
        route.value = "validator"
        launch()
        compose.onNodeWithText("Mês de referência").performClick()
        shot("light-period-picker")
        compose.onNodeWithText("Cancelar").performClick()
        val file = File(context.cacheDir, "visual-fixture.csv")
        val now = LocalDate.now().withDayOfMonth(5)
        file.writeText("data,descricao,valor\n$now,Marina Oliveira,1250\n")
        compose.runOnIdle { validator.selectFile(Uri.fromFile(file), file.name) }
        compose.onNodeWithText("Validar pagamentos").performClick()
        compose.waitUntil(15_000) { validator.uiState.value is ValidatorState.Success }
        shot("light-validation-results")
        compose.onNodeWithContentDescription("Buscar inquilino").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused()
        shot("statement-search-keyboard")
        compose.onNodeWithText("Ver resultados").performClick()
        compose.onNodeWithText("Ana Beatriz de Albuquerque").performClick()
        compose.onNodeWithText("Cobrar").performScrollTo()
        shot("light-validation-pending-actions")
        compose.onNodeWithText("Ana Beatriz de Albuquerque").performClick()
        compose.onNodeWithText("Marina Oliveira").performClick()
        compose.onNodeWithText("Transações encontradas").assertExists()
        shot("light-validation-expanded")
        compose.runOnIdle { dark.value = true }
        shot("dark-validation-expanded")
        compose.onNodeWithText("Registrar pagamento").performScrollTo().performClick()
        shot("dark-register-payment")
        compose.onNodeWithText("Em análise").performClick()
        compose.onNodeWithText("Confirmar").performClick()
        compose.waitUntil(10_000) { grid.state.value.items.any { it.tenant.id == "visual-0" && it.payment?.status == PaymentStatus.EM_ANALISE } }
        file.delete()
    }

    @Test fun rapidMonthChangesKeepOneCoherentPeriodWithoutReloadFlash() {
        route.value = "grid"
        launch()
        compose.waitUntil(10_000) { grid.state.value.items.size == 3 && !grid.state.value.isLoading }
        val start = java.time.YearMonth.of(grid.state.value.year, grid.state.value.month)
        val target = start.minusMonths(4)
        val snapshots = java.util.Collections.synchronizedList(mutableListOf<MonthlyGridState>())
        val watcher = CoroutineScope(Dispatchers.Main.immediate).launch {
            grid.state.collect { snapshots.add(it) }
        }
        try {
            repeat(4) { compose.onNodeWithContentDescription("Mês anterior").performClick() }
            compose.waitUntil(10_000) {
                grid.state.value.year == target.year && grid.state.value.month == target.monthValue
            }
            val observed = synchronized(snapshots) { snapshots.toList() }
            Assert.assertFalse(observed.any { it.isLoading })
            Assert.assertTrue(observed.all { period -> period.items.all { item ->
                item.payment == null || item.payment.year == period.year && item.payment.month == period.month
            } })
            compose.onNodeWithText("Atualizando pagamentos").assertDoesNotExist()
            shot("monthly-rapid-month-switch")
        } finally {
            watcher.cancel()
        }
    }

    @Test fun statementReflectsTenantRentEditsWithoutAddingTheLateFeeToDue() {
        route.value = "validator"
        launch()
        val period = java.time.YearMonth.now().minusMonths(1)
        val file = File(context.cacheDir, "visual-rent-refresh.csv")
        file.writeText("data,descricao,valor\n${period.atDay(5)},Marina Oliveira,100\n")
        try {
            compose.runOnIdle {
                validator.updateReferencePeriod(period)
                validator.selectFile(Uri.fromFile(file), file.name)
            }
            compose.onNodeWithText("Validar pagamentos").performClick()
            compose.waitUntil(15_000) {
                (validator.uiState.value as? ValidatorState.Success)?.results?.any {
                    it.tenant.id == "visual-0" && it.amountDue == 1250.0
                } == true
            }
            runBlocking {
                val current = db.tenantDao().getTenantById("visual-0")!!
                db.tenantDao().updateTenant(current.copy(amount = 900.0))
            }
            compose.waitUntil(15_000) {
                (validator.uiState.value as? ValidatorState.Success)?.results?.any {
                    it.tenant.id == "visual-0" && it.amountDue == 900.0 && it.amountPaid == 100.0
                } == true
            }
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Marina Oliveira"))
            compose.onNodeWithText("Marina Oliveira").performClick()
            compose.onNodeWithText(com.rentalvalidator.app.util.CurrencyUtils.format(900.0)).assertExists()
            compose.onNodeWithText("Incluir multa na cobrança").assertExists()
            shot("statement-current-rent-optional-fee")
            compose.onNode(isToggleable()).performScrollTo().assertIsDisplayed().assertIsOff().performClick().assertIsOn()
            shot("statement-optional-fee-selected")
        } finally {
            file.delete()
        }
    }

    @Test fun unitsFiltersAndSearch() {
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { units.units.value.isNotEmpty() }
        compose.onNodeWithContentDescription("Mostrar inquilinos").performClick()
        shot("light-unit-expanded")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Rafael Mendes"))
        compose.onNodeWithText("Unidades").assertIsDisplayed()
        compose.onNodeWithText("Inquilinos").assertIsDisplayed()
        compose.onNodeWithContentDescription("Recolher inquilinos").performScrollTo().performClick()
        compose.onNodeWithText("Jardim das Oliveiras").performClick()
        compose.onNodeWithContentDescription("Excluir unidade").performClick()
        compose.onNodeWithText("Entendi").assertIsDisplayed().performClick()
        shot("light-unit-detail")
        compose.runOnIdle { dark.value = true }
        shot("dark-unit-detail")
        compose.runOnIdle { fontScale.floatValue = 1.3f }
        shot("large-type-unit-detail")
        compose.runOnIdle { dark.value = false; fontScale.floatValue = 1f }
        compose.onNodeWithContentDescription("Editar detalhes da unidade").performClick()
        compose.onNodeWithText("Salvar").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
        shot("light-unit-edit")
        compose.runOnIdle { dark.value = true }
        shot("dark-unit-edit")
        compose.onNodeWithContentDescription("Tipo").performClick()
        shot("dark-unit-type-menu")
        compose.onAllNodesWithText("Apartamento").onLast().performClick()
        compose.runOnIdle { dark.value = false }
        compose.onNodeWithContentDescription("Fechar").performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithContentDescription("Filtros").performClick()
        shot("light-tenant-filter")
        compose.onNodeWithText("Todas").performClick()
        shot("light-unit-dropdown")
        compose.onAllNodesWithText("Jardim das Oliveiras").onLast().performClick()
        compose.onNodeWithText("Aplicar").performClick()
        compose.onNodeWithContentDescription("Buscar inquilino").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("zzzz-no-result")
        shot("portfolio-search-keyboard")
        compose.onNodeWithText("Ver resultados").performClick()
        shot("light-search-empty")
    }

    /** The registration and the forms open over the page; addressing them inside it keeps the page behind out of the match. */
    private val inEditor = hasAnyAncestor(isDialog())
    private fun editor(matcher: SemanticsMatcher) = compose.onNode(matcher and inEditor)
    private fun editorText(text: String) = editor(hasText(text))
    private fun editorField(label: String) = editor(hasContentDescription(label) and hasSetTextAction())
    /** Focus moves after the step has settled, so the check waits for it instead of reading it once. */
    private fun awaitFocus(label: String) =
        compose.waitUntil("$label focused", 5_000) { runCatching { editorField(label).assertIsFocused() }.isSuccess }
    /**
     * Presses a footer action through its click action. A tap injected right after the keyboard
     * changes layout (text, phone and number pads differ in height) can land beside the button
     * while the editor is still resizing above it.
     */
    private fun press(action: String) { editorText(action).performSemanticsAction(SemanticsActions.OnClick) }
    private fun awaitEditorText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text) and inEditor).fetchSemanticsNodes().isNotEmpty() }

    @Test fun guidedTenantRegistrationKeepsAnswersAndSaves() {
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNodeWithContentDescription("Novo cadastro").performClick()
        editorText("O que você quer cadastrar?").assertIsDisplayed()
        shot("light-registration-choice")
        editor(hasText("Inquilino") and hasClickAction()).performClick()

        editorText("Qual é o nome do inquilino?").assertIsDisplayed()
        press("Avançar")
        editorText("Informe o nome").assertExists()
        awaitFocus("Nome completo")
        editorField("Nome completo").performTextInput("Helena Duarte")
        press("Avançar")

        editorText("Em qual unidade Helena mora?").assertIsDisplayed()
        editor(hasText("Jardim das Oliveiras") and hasClickAction()).performClick()
        editor(hasText("Jardim das Oliveiras") and hasClickAction()).assertIsSelected()
        press("Avançar")

        press("Avançar")
        editorText("Informe um valor válido").assertExists()
        editorText("Escolha o dia de vencimento, de 1 a 31").assertExists()
        awaitFocus("Aluguel mensal")
        shot("light-registration-rent-errors")
        editorField("Aluguel mensal").performTextInput("1250,50")
        editor(hasContentDescription("Dia 10")).performScrollTo().performClick().assertIsSelected()
        press("Avançar")

        editorField("Telefone").performTextInput("119")
        press("Avançar")
        awaitFocus("Telefone")
        editorField("Telefone").performTextReplacement("11987654321")
        editorField("CPF").performScrollTo().performTextInput("11111111111")
        press("Avançar")
        awaitFocus("CPF")
        editorText("Informe um CPF válido com 11 dígitos ou deixe em branco").assertExists()
        editorField("CPF").performTextReplacement("52998224725")
        press("Avançar")

        editor(hasText("Nubank") and hasClickAction()).performScrollTo().performClick()
        // A name typed but not added still counts when the step is left.
        editorField("Outro nome no extrato").performScrollTo().performTextInput("Helena Pix")
        press("Revisar")

        editorText("Confira o cadastro").assertIsDisplayed()
        editorText("Outros nomes: Helena Pix").assertExists()
        shot("light-registration-tenant-review")
        compose.runOnIdle { dark.value = true }
        shot("dark-registration-tenant-review")
        compose.runOnIdle { dark.value = false }

        // An answer opened from the review returns to it, and going back keeps every answer.
        editor(hasText("Nome") and hasClickAction()).performClick()
        editorField("Nome completo").assertTextContains("Helena Duarte").performTextReplacement("Helena Duarte Lima")
        press("Revisar")
        editor(hasText("Nome") and hasText("Helena Duarte Lima") and hasClickAction()).assertExists()
        press("Voltar")
        editorText("Helena Pix").assertExists()
        editor(hasText("Nubank") and hasClickAction()).assertIsSelected()
        press("Revisar")

        press("Cadastrar inquilino")
        compose.waitUntil(10_000) { tenants.tenants.value.size == 4 }
        val saved = tenants.tenants.value.single { it.name == "Helena Duarte Lima" }
        Assert.assertEquals(1250.5, saved.amount, .001)
        Assert.assertEquals(10, saved.dueDay)
        Assert.assertEquals("visual-unit", saved.unitId)
        Assert.assertEquals("Jardim das Oliveiras", saved.unit)
        Assert.assertEquals("11987654321", saved.phone.filter(Char::isDigit))
        Assert.assertEquals("52998224725", saved.cpf)
        Assert.assertEquals("Nubank", saved.bank)
        Assert.assertEquals(listOf("Helena Pix"), saved.aliases)
        compose.waitUntil(5_000) { compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Cadastro de Helena Duarte Lima concluído").assertExists()
    }

    @Test fun guidedUnitRegistrationChecksTheNameAndOffersTheFirstTenant() {
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { units.units.value.isNotEmpty() }
        compose.onNodeWithContentDescription("Novo cadastro").performClick()
        editor(hasText("Unidade") and hasClickAction()).performClick()

        editor(hasText("Kitnet") and hasClickAction()).performClick().assertIsSelected()
        press("Avançar")

        editorText("Como se chama esta kitnet e onde fica?").assertIsDisplayed()
        press("Avançar")
        editorText("Informe o nome da unidade").assertExists()
        awaitFocus("Nome da unidade")
        editorField("Nome da unidade").performTextInput("Jardim das Oliveiras")
        editorField("Endereço").performTextInput("Rua Horizonte, 120")
        press("Avançar")
        awaitEditorText("Já existe uma unidade com este nome.")
        awaitFocus("Nome da unidade")
        editorField("Nome da unidade").performTextReplacement("Residencial Horizonte")
        press("Avançar")

        awaitEditorText("Quanto cabe nesta kitnet?")
        editor(hasContentDescription("Aumentar capacidade")).performClick()
        editorField("Capacidade").assert(hasText("2"))
        editorField("Capacidade").performTextReplacement("0")
        press("Avançar")
        editorText("Mínimo 1").assertExists()
        awaitFocus("Capacidade")
        editorField("Capacidade").performTextReplacement("2")
        press("Avançar")

        editorField("Condomínio mensal (R$)").performScrollTo().performTextInput("1,234")
        press("Revisar")
        awaitFocus("Condomínio mensal (R$)")
        editorText("Informe um valor a partir de zero, com até 2 casas decimais").assertExists()
        editorField("Condomínio mensal (R$)").performTextReplacement("350")
        press("Revisar")

        editorText("Confira a unidade").assertIsDisplayed()
        shot("light-registration-unit-review")
        press("Cadastrar unidade")
        compose.waitUntil(10_000) { units.units.value.size == 2 }
        val saved = units.units.value.single { it.name == "Residencial Horizonte" }
        Assert.assertEquals("Rua Horizonte, 120", saved.location)
        Assert.assertEquals(UnitType.KITNET, saved.type)
        Assert.assertEquals(2, saved.capacity)
        Assert.assertEquals(350.0, saved.condominiumFee!!, .001)

        // The confirmation offers the unit's first tenant, already placed in it.
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Adicionar inquilino") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Unidade Residencial Horizonte cadastrada").assertExists()
        compose.onNode(hasText("Adicionar inquilino") and hasClickAction()).performClick()
        editorText("Qual é o nome do inquilino?").assertIsDisplayed()
        editorField("Nome completo").performTextInput("Teste")
        press("Avançar")
        editor(hasText("Residencial Horizonte") and hasClickAction()).assertIsSelected()
        editor(hasContentDescription("Fechar")).performClick()
        compose.onNodeWithText("Descartar cadastro?").assertIsDisplayed()
        compose.onNodeWithText("Descartar").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
        Assert.assertEquals(3, tenants.tenants.value.size)

        compose.onNodeWithText("Residencial Horizonte").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Excluir unidade").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        Assert.assertEquals(2, units.units.value.size)
        compose.onNodeWithContentDescription("Excluir unidade").performClick()
        compose.onNodeWithText("Excluir", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { units.units.value.size == 1 }
    }

    @Test fun tenantEditFormFocusesInvalidFieldsInOrder() {
        route.value = "tenants"
        launch()
        compose.waitUntil(10_000) { tenants.tenants.value.size == 3 }
        compose.onNodeWithText("Inquilinos").performClick()
        compose.onNodeWithText("Marina Oliveira").performClick()
        compose.onNodeWithContentDescription("Editar inquilino").performClick()
        editorField("Nome completo").performTextReplacement("")
        editorField("Telefone").performScrollTo().performTextReplacement("119")
        editorField("CPF (opcional)").performScrollTo().performTextReplacement("11111111111")
        editorField("Aluguel mensal").performScrollTo().performTextReplacement("")
        editorField("Dia de vencimento").performScrollTo().performTextReplacement("")
        press("Salvar")
        awaitFocus("Nome completo")
        editorField("Nome completo").performTextInput("Marina Oliveira")
        press("Salvar")
        awaitFocus("Telefone")
        editorField("Telefone").performTextReplacement("11987654321")
        press("Salvar")
        awaitFocus("CPF (opcional)")
        shot("invalid-cpf-focused")
        editorField("CPF (opcional)").performTextReplacement("52998224725")
        press("Salvar")
        awaitFocus("Aluguel mensal")
        editorField("Aluguel mensal").performTextInput("1250,50")
        press("Salvar")
        awaitFocus("Dia de vencimento")
        shot("invalid-due-focused")
        press("Cancelar")
        Assert.assertEquals(1250.0, tenants.tenants.value.single { it.id == "visual-0" }.amount, .001)
    }

    @Test fun documentDossierAndFilters() {
        route.value = "contracts"
        launch()
        compose.waitUntil(10_000) { contracts.tenants.value.size == 3 }
        compose.onNodeWithContentDescription("Buscar inquilino").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused()
        shot("documents-search-keyboard")
        compose.onNodeWithText("Ver resultados").performClick()
        compose.onNodeWithText("Marina Oliveira").performClick()
        shot("light-document-dossier")
        compose.onNodeWithText("Vencimento do contrato").performClick()
        shot("light-contract-date")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithContentDescription("Filtros").performClick()
        shot("light-contract-filter")
        compose.onNodeWithText("Aplicar").performClick()
    }

    @Test fun validationErrorAndPdf() {
        route.value = "validator"
        launch()
        compose.runOnIdle { validator.selectFile(Uri.fromFile(File(context.cacheDir, "missing.pdf")), "missing.pdf") }
        compose.onNodeWithText("Validar pagamentos").performClick()
        compose.waitUntil(10_000) { validator.uiState.value is ValidatorState.Error }
        shot("light-validation-error")
        compose.onNodeWithText("Selecionar outro extrato").performClick()
        val pdf = File(context.cacheDir, "visual-fixture.pdf")
        val document = android.graphics.pdf.PdfDocument()
        try {
            val page = document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(400, 600, 1).create())
            val date = LocalDate.now().withDayOfMonth(5).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            page.canvas.drawText("$date Pix recebido Marina Oliveira 1.250,00", 20f, 40f,
                android.graphics.Paint().apply { textSize = 12f })
            document.finishPage(page)
            pdf.outputStream().use(document::writeTo)
        } finally { document.close() }
        compose.runOnIdle { validator.selectFile(Uri.fromFile(pdf), pdf.name) }
        compose.onNodeWithText("Validar pagamentos").performClick()
        compose.waitUntil(15_000) { validator.uiState.value is ValidatorState.Success }
        shot("light-pdf-results")
        pdf.delete()
    }
}
