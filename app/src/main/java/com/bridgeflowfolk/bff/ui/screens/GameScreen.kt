package com.bridgeflowfolk.bff.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.*

// ── Palette propre au jeu ────────────────────────────────────────────────────

private val Sage     = Color(0xFF7A9E87)
private val SageL    = Color(0xFFA8C4B0)
private val Cream    = Color(0xFFF5F0E8)
private val Warm     = Color(0xFFE8DDD0)
private val Brown    = Color(0xFF6B5240)
private val Gold     = Color(0xFFC9A96E)
private val GoldL    = Color(0xFFE8D5B0)
private val Dark     = Color(0xFF2D2420)

// ── Données du jeu ───────────────────────────────────────────────────────────

private val WORD_POOLS = listOf(
    listOf("BRIDGE","FLOW","FOLK","BIENETRE","LIEN","PARTAGE","SOIN","NATURE","CREATEURS","EVEIL"),
    listOf("BRIDGE","FLOW","FOLK","HARMONIE","ECHANGE","ATELIER","ANCRAGE","VITALITE","COMMUNAUTE","SENS"),
    listOf("BRIDGE","FLOW","FOLK","EQUILIBRE","RENCONTRE","TERRITOIRE","MOUVEMENT","HUMANITE","RACINES","EVEIL"),
)
private val SECRET_WORDS = listOf("HUMANITE","TERRITOIRE","HARMONIE","EQUILIBRE","ANCRAGE")

private val DIRECTIONS = listOf(
    0 to 1, 1 to 0, 1 to 1, 1 to -1,
    0 to -1, -1 to 0, -1 to -1, -1 to 1
)
private const val GRID_SIZE = 12

// ── Modèles ─────────────────────────────────────────────────────────────────

data class PlacedWord(val word: String, val cells: List<Pair<Int,Int>>)

data class GameState(
    val grid: List<List<Char>>,
    val placed: List<PlacedWord>,
    val wordStatus: Map<String, Boolean>,   // mot → trouvé?
    val secretWord: String,
    val foundCount: Int = 0,
    val score: Int = 0,
    val elapsedSec: Int = 0,
    val won: Boolean = false
)

// ── Logique de génération ────────────────────────────────────────────────────

private fun buildGame(): GameState {
    val pool = WORD_POOLS.random().shuffled()
    val secret = SECRET_WORDS.random()
    val toPlace = pool.take(6)

    val g = Array(GRID_SIZE) { CharArray(GRID_SIZE) { ' ' } }
    val placed = mutableListOf<PlacedWord>()

    fun canPlace(word: String, r: Int, c: Int, dr: Int, dc: Int): Boolean {
        word.forEachIndexed { i, ch ->
            val nr = r + dr * i; val nc = c + dc * i
            if (nr !in 0 until GRID_SIZE || nc !in 0 until GRID_SIZE) return false
            if (g[nr][nc] != ' ' && g[nr][nc] != ch) return false
        }
        return true
    }

    fun doPlace(word: String, r: Int, c: Int, dr: Int, dc: Int): PlacedWord {
        val cells = mutableListOf<Pair<Int,Int>>()
        word.forEachIndexed { i, ch ->
            val nr = r + dr * i; val nc = c + dc * i
            g[nr][nc] = ch; cells += nr to nc
        }
        return PlacedWord(word, cells)
    }

    fun tryPlace(word: String): Boolean {
        repeat(120) {
            val (dr, dc) = DIRECTIONS.random()
            val r = (0 until GRID_SIZE).random()
            val c = (0 until GRID_SIZE).random()
            if (canPlace(word, r, c, dr, dc)) { placed += doPlace(word, r, c, dr, dc); return true }
        }
        return false
    }

    tryPlace(secret)
    val wordStatus = mutableMapOf<String, Boolean>()
    toPlace.forEach { w -> if (tryPlace(w)) wordStatus[w] = false }

    val alpha = "ABCDEFGHIJKLMNOPRSTUVW"
    for (r in 0 until GRID_SIZE) for (c in 0 until GRID_SIZE)
        if (g[r][c] == ' ') g[r][c] = alpha.random()

    return GameState(
        grid       = g.map { it.toList() },
        placed     = placed,
        wordStatus = wordStatus,
        secretWord = secret
    )
}

// ── Ligne droite entre deux cellules ─────────────────────────────────────────

private fun lineCells(r0: Int, c0: Int, r1: Int, c1: Int): List<Pair<Int,Int>> {
    val dr = r1 - r0; val dc = c1 - c0
    val len = max(abs(dr), abs(dc))
    if (len == 0) return listOf(r0 to c0)
    val sr = if (dr == 0) 0 else dr / abs(dr)
    val sc = if (dc == 0) 0 else dc / abs(dc)
    // force axe dominant si non-diagonal
    return if (abs(dr) != abs(dc) && dr != 0 && dc != 0) {
        if (abs(dr) > abs(dc)) (0..abs(dr)).map { r0 + sr * it to c0 }
        else (0..abs(dc)).map { r0 to c0 + sc * it }
    } else {
        (0..len).map { r0 + sr * it to c0 + sc * it }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SCREEN PRINCIPAL
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun GameScreen() {
    var state by remember { mutableStateOf(buildGame()) }
    var selection by remember { mutableStateOf<List<Pair<Int,Int>>>(emptyList()) }
    var foundCells by remember { mutableStateOf<Set<Pair<Int,Int>>>(emptySet()) }
    var showRules by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(state.won, state.elapsedSec) {
        if (!state.won) {
            delay(1000)
            state = state.copy(elapsedSec = state.elapsedSec + 1)
        }
    }

    // Victoire auto-detect
    LaunchedEffect(state.wordStatus) {
        if (state.wordStatus.isNotEmpty() && state.wordStatus.values.all { it } && !state.won) {
            delay(500)
            // Révéler les cellules du mot secret
            val sp = state.placed.find { it.word == state.secretWord }
            if (sp != null) foundCells = foundCells + sp.cells.toSet()
            state = state.copy(won = true)
        }
    }

    fun newGame() {
        state = buildGame()
        selection = emptyList()
        foundCells = emptySet()
    }

    fun onSelectionEnd(cells: List<Pair<Int,Int>>) {
        if (cells.size < 2) return
        val word    = cells.joinToString("") { (r, c) -> state.grid[r][c].toString() }
        val wordRev = word.reversed()

        val match = state.placed.firstOrNull { p ->
            (p.word == word || p.word == wordRev) &&
            p.cells.sortedWith(compareBy({ it.first }, { it.second })) ==
            cells.sortedWith(compareBy({ it.first }, { it.second }))
        } ?: return

        val w = match.word
        if (!state.wordStatus.containsKey(w) || state.wordStatus[w] == true) return

        val bonus  = max(10, 50 - state.elapsedSec / 3)
        foundCells = foundCells + match.cells.toSet()
        state = state.copy(
            wordStatus = state.wordStatus + (w to true),
            foundCount = state.foundCount + 1,
            score      = state.score + bonus
        )
        // Révélation progressive du mot secret
        val total   = state.wordStatus.size
        val found   = state.wordStatus.values.count { it } + 1  // +1 déjà compté
        // (géré via revealCount dans le composable)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Score bar ────────────────────────────────────────────────────
        ScoreBar(
            found      = state.foundCount,
            total      = state.wordStatus.size,
            score      = state.score,
            elapsed    = state.elapsedSec,
            onNewGame  = { newGame() },
            onRules    = { showRules = true }
        )

        Spacer(Modifier.height(8.dp))

        // ── Grille ───────────────────────────────────────────────────────
        WordGrid(
            grid       = state.grid,
            selection  = selection,
            foundCells = foundCells,
            onSelChange = { selection = it },
            onSelEnd    = { cells ->
                onSelectionEnd(cells)
                selection = emptyList()
            }
        )

        Spacer(Modifier.height(12.dp))

        // ── Mot secret ───────────────────────────────────────────────────
        val revealCount = if (state.wordStatus.isEmpty()) 0 else
            (state.foundCount.toFloat() / state.wordStatus.size * state.secretWord.length).toInt()
                .coerceIn(0, state.secretWord.length)

        SecretWordRow(
            word        = state.secretWord,
            revealCount = if (state.won) state.secretWord.length else revealCount
        )

        Spacer(Modifier.height(12.dp))

        // ── Liste des mots ────────────────────────────────────────────────
        WordChips(wordStatus = state.wordStatus)

        Spacer(Modifier.height(24.dp))
    }

    // ── Overlay victoire ──────────────────────────────────────────────────
    if (state.won) {
        VictoryDialog(
            secretWord  = state.secretWord,
            score       = state.score,
            elapsed     = state.elapsedSec,
            onNewGame   = { newGame() }
        )
    }

    // ── Règles ────────────────────────────────────────────────────────────
    if (showRules) {
        RulesDialog(onDismiss = { showRules = false })
    }
}

// ── Score bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ScoreBar(
    found: Int, total: Int, score: Int, elapsed: Int,
    onNewGame: () -> Unit, onRules: () -> Unit
) {
    Surface(color = Warm, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatChip(label = "Mots",  value = "$found/$total")
            StatChip(label = "Score", value = "$score", valueColor = Gold)
            StatChip(label = "Temps", value = "${elapsed}s", valueColor = Sage)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onRules, modifier = Modifier.size(36.dp)) {
                    Text("?", fontWeight = FontWeight.Bold, color = Brown, fontSize = 16.sp)
                }
                IconButton(
                    onClick = onNewGame,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Gold)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rejouer",
                        tint = Dark, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, valueColor: Color = Dark) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, color = Brown,
            fontWeight = FontWeight.Medium)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── Grille ───────────────────────────────────────────────────────────────────

@Composable
private fun WordGrid(
    grid: List<List<Char>>,
    selection: List<Pair<Int,Int>>,
    foundCells: Set<Pair<Int,Int>>,
    onSelChange: (List<Pair<Int,Int>>) -> Unit,
    onSelEnd: (List<Pair<Int,Int>>) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        val cellPx = ((maxWidth - 12.dp) / GRID_SIZE).coerceAtMost(36.dp)

        // CORRECTION 1 : On récupère la densité courante correctement
        val density = LocalDensity.current 
        val cellPxValue = with(density) { cellPx.toPx() }

        // CORRECTION 2 : On supprime 'cellPositions' qui était déclaré mais jamais utilisé
        var startCell by remember { mutableStateOf<Pair<Int,Int>?>(null) }

        fun cellAt(offset: Offset): Pair<Int,Int>? {
            // CORRECTION 3 : Utilisation de cellPxValue pré-calculé
            val col = (offset.x / cellPxValue).toInt().coerceIn(0, GRID_SIZE - 1)
            val row = (offset.y / cellPxValue).toInt().coerceIn(0, GRID_SIZE - 1)
            return row to col
        }

        Column(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val c = cellAt(offset)
                            startCell = c
                            onSelChange(listOfNotNull(c))
                        },
                        onDrag = { change, _ ->
                            val c = cellAt(change.position)
                            val s = startCell ?: return@detectDragGestures
                            onSelChange(lineCells(s.first, s.second, c.first, c.second))
                        },
                        onDragEnd = {
                            val cur = selection
                            startCell = null
                            onSelEnd(cur)
                        },
                        onDragCancel = {
                            startCell = null
                            onSelChange(emptyList())
                        }
                    )
                }
        ) {
            repeat(GRID_SIZE) { r ->
                Row {
                    repeat(GRID_SIZE) { c ->
                        val coord = r to c
                        val isSelected = coord in selection
                        val isFound    = coord in foundCells

                        val bgColor by animateColorAsState(
                            targetValue = when {
                                isFound    -> Sage
                                isSelected -> Gold
                                else       -> Cream
                            },
                            animationSpec = tween(120), label = "cell_bg"
                        )
                        val textColor = when {
                            isFound || isSelected -> Color.White
                            else -> Dark
                        }
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected && !isFound) 1.08f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessHigh),
                            label = "cell_scale"
                        )

                        Box(
                            modifier = Modifier
                                .size(cellPx)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(RoundedCornerShape(5.dp))
                                .background(bgColor)
                                .border(1.dp, if (isFound || isSelected) Color.Transparent else GoldL,
                                    RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text      = grid[r][c].toString(),
                                fontSize  = (cellPx.value * 0.40f).sp,
                                fontWeight = FontWeight.Bold,
                                color     = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Mot secret ────────────────────────────────────────────────────────────────

@Composable
private fun SecretWordRow(word: String, revealCount: Int) {
    Surface(
        color = Dark,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🔑 Mot mystère — révélé lettre par lettre",
                fontSize = 10.sp, letterSpacing = 1.sp,
                color = SageL, modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.wrapContentWidth()) {
                word.forEachIndexed { i, ch ->
                    val revealed = i < revealCount
                    val bgAnim by animateColorAsState(
                        if (revealed) Gold else Color(0x14FFFFFF),
                        tween(300), label = "sb_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 34.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(bgAnim)
                            .border(1.dp, if (revealed) Gold else Color(0x4DC9A96E),
                                RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (revealed) ch.toString() else "",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = Dark
                        )
                    }
                }
            }
        }
    }
}

// ── Chips de mots ─────────────────────────────────────────────────────────────

@Composable
private fun WordChips(wordStatus: Map<String, Boolean>) {
    Column(modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
        Text(
            "Mots à trouver",
            fontSize = 9.sp, letterSpacing = 1.5.sp, color = Brown,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .drawBehind {
                    drawLine(color = GoldL, start = Offset(0f, size.height + 4),
                        end = Offset(size.width, size.height + 4), strokeWidth = 1f)
                }
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            wordStatus.forEach { (word, found) ->
                val bgAnim by animateColorAsState(
                    if (found) Sage else Cream, tween(300), label = "chip_$word"
                )
                val textColor = if (found) Color.White else Brown
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = bgAnim,
                    border = BorderStroke(1.5.dp, if (found) Sage else GoldL),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = word,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, color = textColor,
                        textDecoration = if (found) TextDecoration.LineThrough else null,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// ── Victoire ──────────────────────────────────────────────────────────────────

@Composable
private fun VictoryDialog(secretWord: String, score: Int, elapsed: Int, onNewGame: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Cream,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🌿", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("Bravo !", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Dark)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tu as trouvé tous les mots.\nLe mot mystère était :",
                    fontSize = 13.sp, color = Brown, textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    secretWord,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = Gold, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Score : $score pts · Temps : ${elapsed}s",
                    fontSize = 12.sp, color = Brown
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onNewGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null,
                        tint = Dark, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nouvelle partie", color = Dark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Règles du jeu ─────────────────────────────────────────────────────────────

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Cream,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text("📖 Règles du jeu", fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = Dark)
                Spacer(Modifier.height(14.dp))
                RuleItem("👆", "Glisse le doigt sur la grille pour sélectionner des lettres.")
                RuleItem("➡️", "Les mots peuvent être lus horizontalement, verticalement ou en diagonale — dans les deux sens.")
                RuleItem("🔑", "Chaque mot trouvé révèle une lettre du mot mystère caché.")
                RuleItem("🌿", "Trouve tous les mots pour révéler le mot mystère et gagner !")
                RuleItem("⚡", "Plus tu es rapide, plus ton score est élevé.")
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("C'est parti !", color = Dark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RuleItem(emoji: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text, fontSize = 13.sp, color = Brown, lineHeight = 19.sp,
            modifier = Modifier.weight(1f))
    }
}
