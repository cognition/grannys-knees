package ca.aeo3.grannysknees.sayings

import kotlin.random.Random

/**
 * Picks Granny lines for the current weather-index score.
 *
 * A new score resets the wry climb. Refresh raises wry preference and draws
 * again, avoiding an immediate text repeat when another line exists.
 */
internal class GrannySaysPicker(
    private val bandForScore: (Int) -> List<GrannySaying>,
    private val random: Random = Random.Default,
) {
    constructor(
        band: List<GrannySaying>,
        random: Random = Random.Default,
    ) : this({ band }, random)

    constructor(
        catalog: GrannySaysCatalog,
        random: Random = Random.Default,
    ) : this(catalog::sayingsForScore, random)

    /** Number of refreshes since the current score was set. Zero on first show. */
    var refreshIndex: Int = 0
        private set

    private var score: Int? = null
    private var personalScore: Int? = null
    private var freeSpeechEnabled: Boolean = false
    private var season: GrannySeason = GrannySeason.ALL_YEAR
    private var current: GrannySaying? = null

    /** Weights used for the next pick of the current score. */
    fun currentWeights(): ToneWeights = ToneWeights.forRefreshIndex(refreshIndex)

    /**
     * Shows a saying for [score].
     *
     * When the score is unchanged, returns the existing line. When it changes,
     * resets the wry climb and picks again.
     */
    fun showForScore(
        score: Int,
        personalScore: Int? = null,
        allowFreeSpeech: Boolean = false,
        season: GrannySeason = GrannySeason.ALL_YEAR,
        snowInForecast: Boolean = false,
    ): GrannySaying? {
        val nextSeason = if (snowInForecast) GrannySeason.WINTER else season
        if (this.score == score && this.personalScore == personalScore &&
            freeSpeechEnabled == allowFreeSpeech && this.season == nextSeason && current != null
        ) {
            return current
        }
        this.score = score
        this.personalScore = personalScore
        freeSpeechEnabled = allowFreeSpeech
        this.season = nextSeason
        refreshIndex = 0
        current = pick(currentWeights(), excludeText = null)
        return current
    }

    /** Picks another line for the current score, climbing wry preference. */
    fun refresh(): GrannySaying? {
        if (score == null) {
            return null
        }
        refreshIndex += 1
        current = pick(currentWeights(), excludeText = current?.text)
        return current
    }

    /**
     * Draws one saying using [weights], skipping [excludeText] when another
     * line is available in the chosen pool or the full band.
     */
    fun pick(weights: ToneWeights, excludeText: String?): GrannySaying? {
        val band = eligibleBand(bandForScore(score ?: 1))
        if (band.isEmpty()) {
            return null
        }
        val tone = drawTone(weights, band) ?: return band.random(excludeText)
        val inTone = band.filter { saying -> saying.tone == tone }
        return inTone.random(excludeText) ?: band.random(excludeText)
    }

    private fun eligibleBand(band: List<GrannySaying>): List<GrannySaying> {
        val standard = score
        val personal = personalScore
        val personalPresent = personal != null && standard != null
        val personalDiffers = personalPresent && personal != standard
        val personalAgrees = personalPresent && personal == standard
        val eligible = band.filter { saying ->
            seasonMatches(saying) &&
                (!saying.requiresPersonalAgreement || personalAgrees) &&
                (!saying.requiresPersonalDifference || personalDiffers) &&
                (saying.expectedPersonalScore == null || saying.expectedPersonalScore == personal) &&
                (!saying.requiresFreeSpeech || freeSpeechEnabled)
        }
        val clean = band.filter { saying ->
            seasonMatches(saying) &&
                !saying.requiresFreeSpeech &&
                !saying.requiresPersonalAgreement &&
                !saying.requiresPersonalDifference
        }
        return eligible.ifEmpty { clean.ifEmpty { band.filter(::seasonMatches).ifEmpty { band } } }
    }

    private fun seasonMatches(saying: GrannySaying): Boolean =
        saying.season == GrannySeason.ALL_YEAR || saying.season == season

    private fun drawTone(weights: ToneWeights, band: List<GrannySaying>): GrannyTone? {
        val present = buildList {
            if (band.any { it.tone == GrannyTone.WRY } && weights.wry > 0) {
                add(GrannyTone.WRY to weights.wry)
            }
            if (band.any { it.tone == GrannyTone.WARM } && weights.warm > 0) {
                add(GrannyTone.WARM to weights.warm)
            }
            if (band.any { it.tone == GrannyTone.SPICY } && weights.spicy > 0) {
                add(GrannyTone.SPICY to weights.spicy)
            }
        }
        if (present.isEmpty()) {
            return null
        }
        val total = present.sumOf { pair -> pair.second }
        var ticket = random.nextInt(total)
        present.forEach { (tone, weight) ->
            if (ticket < weight) {
                return tone
            }
            ticket -= weight
        }
        return present.last().first
    }

    private fun List<GrannySaying>.random(excludeText: String?): GrannySaying? {
        if (isEmpty()) {
            return null
        }
        val alternatives = if (excludeText == null) {
            this
        } else {
            filter { saying -> saying.text != excludeText }.ifEmpty { this }
        }
        return alternatives[random.nextInt(alternatives.size)]
    }
}
