package com.example.hydrotracker.logic

import java.time.LocalDate

data class HealthTip(val id: String, val emoji: String, val title: String, val text: String,
                     val tags: Set<String> = emptySet())

val HEALTH_TIPS = listOf(
    HealthTip("wake", "🌅", "Water before coffee", "You lose roughly 500 ml overnight through breath and sweat. A glass on waking refills the tank before caffeine joins in.", setOf("morning", "general")),
    HealthTip("coffee", "☕", "Coffee still counts", "A normal cup's mild diuretic effect is smaller than the water it delivers. Your morning brew adds to your daily total.", setOf("caffeine", "general")),
    HealthTip("food", "🍉", "Food is hydration too", "Around 20% of daily fluid comes from food — fruit, vegetables, soup, yogurt, even oatmeal all count.", setOf("general")),
    HealthTip("thirst", "🎯", "Thirst lags behind", "By the time you feel thirsty you're already mildly down. Scheduled sips beat waiting on busy days.", setOf("work", "general")),
    HealthTip("preload", "🏃", "Front-load before workouts", "Drink 400–500 ml in the 2 hours before exercise. Starting topped up beats playing catch-up mid-session.", setOf("exercise")),
    HealthTip("twopct", "⚖️", "The 2% rule", "Just 2% body-weight loss in sweat measurably drops endurance, strength and focus. Weigh in around long sessions.", setOf("exercise")),
    HealthTip("salt", "🧂", "Sweat long → add salt", "Past ~90 minutes of sweating you lose sodium, not just water. Sports drink, salty snack, or an electrolyte tab.", setOf("exercise", "hot")),
    HealthTip("urine", "🥤", "Check the color", "Pale straw = hydrated. Dark apple-juice shade = drink now. The cheapest hydration gauge you own.", setOf("general")),
    HealthTip("winter", "❄️", "Winter counts too", "Cold air is dry, and heavy layers make you sweat invisibly. Keep the habit when the temperature drops.", setOf("general")),
    HealthTip("flight", "✈️", "Cabin air is a desert", "Aircraft cabins sit near 10–20% humidity — drier than most deserts. Hence the 250 ml per hour rule.", setOf("travel")),
    HealthTip("oneforone", "🍺", "The 1-for-1 rule", "Alcohol dials up urine output. One glass of water per drink keeps tomorrow's head clearer.", setOf("night")),
    HealthTip("temp", "🧊", "Whatever temp you'll drink", "Cold water tastes better, so you drink more. The right temperature is the one that empties the bottle.", setOf("exercise", "general")),
    HealthTip("sick", "🤒", "Sick days need more", "Fever, runny nose and vomiting all drain fluid. Small, frequent sips — even when a full glass feels like too much.", setOf("general")),
    HealthTip("altitude", "🏔️", "Altitude is sneaky", "Higher elevation means faster breathing and more urine output. Add ~500 ml on mountain days.", setOf("travel")),
    HealthTip("slump", "🧠", "The 3 pm slump", "Headache, fog, irritability? Often mild dehydration, not low caffeine. A glass takes 30 seconds.", setOf("work")),
    HealthTip("flavor", "🍋", "Make it interesting", "Lemon, cucumber, mint, berries or sparkling water hydrate exactly like plain water. Boring is the enemy.", setOf("general")),
    HealthTip("tea", "🫖", "Herbal tea = water", "Caffeine-free tea counts one-to-one. A pot of chamomile quietly banks 400 ml.", setOf("general")),
    HealthTip("stack", "⏰", "Stack the habit", "Attach a glass to something you already do: every bathroom break, every meeting start, every episode.", setOf("work", "general")),
    HealthTip("sip", "💧", "Sip, don't chug", "Kidneys process roughly 0.8–1 L per hour. Steady sipping hydrates better than one heroic bottle.", setOf("general")),
    HealthTip("visible", "🚰", "Keep it visible", "Water you can see is water you drink. Desk bottle, bedside glass, bag bottle — cues beat willpower.", setOf("general", "work")),
    HealthTip("heat", "🥵", "Heat doubles the bill", "Hot or humid days can double sweat rates. Use the hot-day toggle and sip before thirst shows up.", setOf("hot")),
    HealthTip("bedtime", "😴", "Time your last glass", "A big glass right before bed buys bathroom trips. Taper the last hour, then a full glass on waking.", setOf("night", "morning")),
    HealthTip("hypo", "⚠️", "More isn't always better", "Drinking far past thirst during long events can dilute blood sodium (hyponatremia). Follow the plan, not endless refills.", setOf("exercise")),
    HealthTip("eightglasses", "8️⃣", "Forget '8 glasses'", "There's no magic number — needs vary with body size, weather and activity. Your goal, pale-straw urine and steady energy are better guides than a fixed count.", setOf("general")),
    HealthTip("lemonmyth", "🌞", "Lemon water isn't magic", "Warm lemon water is lovely, but the benefits come from the water. The lemon adds flavor — which, to be fair, helps you drink more.", setOf("morning", "general")),
    HealthTip("detox", "🧬", "Your body already detoxes", "No drink 'detoxes' you — your liver and kidneys do that around the clock. Water supports them; fancy blends mostly support marketing.", setOf("general")),
    HealthTip("skin", "🧴", "Skin: be realistic", "Severe dehydration dries skin out, but drinking past your needs won't 'glow' you. Moisturizer and sleep do more than a third liter.", setOf("general")),
    HealthTip("desksalt", "🍟", "You don't need electrolytes at your desk", "On a normal day, meals already supply the sodium you need. Save electrolyte drinks for long, sweaty sessions — that's what they're for.", setOf("work", "general")),
    HealthTip("hunger", "🍽️", "Thirst masquerades as hunger", "Mild thirst often shows up as a snack craving. Drink a glass, wait ten minutes, then decide — you'll skip plenty of pointless grazing.", setOf("general", "work")),
    HealthTip("mood", "😤", "In a funk? Water first", "Studies link even mild dehydration to worse mood, focus and headaches. Before blaming your inbox, try a glass.", setOf("work", "general")),
    HealthTip("premeal", "🍲", "A glass before meals", "A 500 ml glass about 30 minutes before eating can modestly reduce how much you eat — and it counts toward your total either way.", setOf("general")),
    HealthTip("bathroom", "🕓", "The every-3-hours rhythm", "Aim for pale straw urine every 3–4 waking hours. Totally clear all day means ease off; rarely going means catch up.", setOf("general")),
    HealthTip("bodywater", "🌊", "You are mostly water", "About 60% of you is water — lungs ~83%, brain and heart ~73%, even bones ~31%. Every sip is maintenance on all of it.", setOf("general")),
    HealthTip("stones", "💎", "Stone formers: drink up", "If you've had kidney stones, steady high fluid intake is the best-proven prevention — dark urine is your warning light. Follow your doctor's target.", setOf("general")),
    HealthTip("uti", "🚽", "Flow is protection", "More fluid in means more flushing out — higher intake is linked to fewer repeat urinary tract infections in people prone to them.", setOf("general")),
    HealthTip("fiber", "🌾", "Fiber's best friend", "Fiber without fluid is like dry pasta — it just sits there. Water keeps things moving, especially while you're increasing fiber.", setOf("general")),
    HealthTip("meds", "💊", "Meds change the math", "Diuretics, some blood-pressure pills and laxatives shift your fluid balance. On regular medication? Ask your pharmacist how it affects hydration.", setOf("general")),
    HealthTip("older", "👵", "Thirst fades with age", "The thirst signal weakens as we get older, so relying on it fails quietly. Scheduled drinks — with meals, meds and TV — beat waiting.", setOf("general")),
    HealthTip("kids", "🧒", "Kids run hotter", "Children heat up faster and forget to drink mid-play. Offer water at every break — a named bottle they decorated helps.", setOf("general")),
    HealthTip("pregnancy", "🤰", "Growing a person", "Pregnancy adds roughly 300 ml a day to fluid needs; breastfeeding around 700 ml. Thirst usually rises to meet it — let it.", setOf("general")),
    HealthTip("sportsdrink", "⚡", "Sports drinks earn their keep late", "Under an hour of effort, water wins. Past ~90 minutes, or in serious heat, carbs plus sodium genuinely earn their place.", setOf("exercise")),
    HealthTip("cramps", "🦵", "Cramps: hydration is one suspect", "Exercise cramps have several causes — fatigue, sodium loss, pacing. Fluid and salt are worth checking first, but stretching and pacing matter too.", setOf("exercise")),
    HealthTip("voluntary", "⏱️", "Thirst lies mid-workout", "During exercise you typically replace only about half of what you sweat out if you wait for thirst. The sip schedule on your session screen exists for exactly this.", setOf("exercise")),
    HealthTip("sweatrange", "📏", "Sweat rates vary 5×", "Sweat rates range from roughly 0.5 to 2.5 liters per hour between people. Your calculator result beats any general table.", setOf("exercise")),
    HealthTip("recoverytime", "⏳", "Recovery takes hours, not minutes", "Rehydrating isn't instant — plan on about 150% of what you lost, spread over the next few hours. That's what the recovery reminders schedule for you.", setOf("exercise")),
    HealthTip("streams", "🚱", "Streams aren't drinking fountains", "Trail water isn't drinking water — treat it, filter it, or carry it. Hiking plans should start from liters needed, not liters hoped for.", setOf("travel")),
    HealthTip("pool", "🏊", "You sweat in the pool", "Water hides sweat — and the effort of swimming. You can finish a session soaked and still be a liter low.", setOf("travel", "hot")),
    HealthTip("ski", "⛷️", "Snow is a desert too", "Cold, altitude, sun reflection and bundled-up sweating: ski days drain fluid almost like beach days. A thermos of tea counts.", setOf("travel")),
    HealthTip("roadtrip", "🚗", "Cupholder bottle", "Long drives are quiet dehydrators — dry cabin air plus 'I don't want a rest stop'. Keep a bottle in the cupholder, and stop anyway.", setOf("travel")),
    HealthTip("ac", "🌬️", "AC and heating dry the air", "Air-conditioned and heated rooms pull moisture from the air — and from you. Desk work in a dry office is a slow, invisible drain.", setOf("work")),
    HealthTip("meeting", "🗓️", "One glass per meeting", "Back-to-back calls are a hydration desert. Make 'one glass per meeting' the rule and the workday takes care of itself.", setOf("work")),
    HealthTip("hangover", "🥴", "Morning-after strategy", "The hangover headache is mostly dehydration plus bad sleep. Water before bed and a big glass on waking genuinely help — electrolytes if it was a big night.", setOf("night")),
    HealthTip("drymouth", "😪", "Dry mouth at 3 AM?", "Waking parched usually means dry bedroom air or mouth-breathing, not a failed day. A bedside glass — and a humidifier in winter — both help.", setOf("night", "morning")),
)

fun tipOfTheDay(date: LocalDate = LocalDate.now()): Int {
    val seed = date.dayOfYear + date.year * 366
    return seed % HEALTH_TIPS.size
}

private val TEMPLATE_TAGS = mapOf(
    "running" to "exercise", "cycling" to "exercise", "gym" to "exercise",
    "yoga" to "exercise", "walking" to "exercise", "swimming" to "exercise",
    "yard" to "exercise", "desk" to "work", "gaming" to "work",
    "commute" to "travel", "hike" to "travel", "flight" to "travel",
    "sauna" to "hot", "night" to "night"
)

fun contextualTips(hotDay: Boolean, activeDay: Boolean, hour: Int,
                   sessionTemplateId: String? = null, limit: Int = 3): List<HealthTip> {
    val todayTipId = HEALTH_TIPS[tipOfTheDay()].id
    val wanted = buildSet {
        if (hotDay) add("hot")
        if (activeDay) add("exercise")
        if (hour in 0..10) add("morning")
        if (hour in 9..17) add("work")
        if (hour >= 18) add("night")
        sessionTemplateId?.let { TEMPLATE_TAGS[it] }?.let(::add)
    }
    val pool = HEALTH_TIPS.filter { it.id != todayTipId && it.tags.any { tag -> tag in wanted } }
    if (pool.isEmpty()) return HEALTH_TIPS.filter { it.id != todayTipId }.take(limit)
    val offset = LocalDate.now().dayOfYear % pool.size
    return List(pool.size) { pool[(it + offset) % pool.size] }.take(limit)
}
