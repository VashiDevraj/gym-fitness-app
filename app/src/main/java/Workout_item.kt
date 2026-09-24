package com.example.gymapplication

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class WorkoutItem(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var gifUrl: String = "",          // public URL (Giphy/Tenor/imgur)
    var gifBase64: String = "",       // base64 encoded image from gallery (stored in Realtime DB)
    var sets: String = "",
    var reps: String = "",
    var caloriesBurn: Int = 0,
    var goal: String = "",
    var category: String = "",
    var durationMinutes: Int = 0,
    var difficulty: String = "Beginner",
    var musclesTargeted: String = "",
    var steps: String = "",
    var breathingTip: String = "",
    var pointsToRemember: String = "",
    var equipment: String = "",
    var emoji: String = "🏋️",
    var isActive: Boolean = true,
    var createdAt: Long = 0L
) {
    constructor() : this("", "", "", "", "", "", "", 0, "", "", 0, "Beginner", "", "", "", "", "", "🏋️", true, 0L)

    /** Returns true if there is any visual (URL or base64) to show */
    fun hasVisual(): Boolean = gifUrl.isNotBlank() || gifBase64.isNotBlank()

    /** Returns the best available visual source for Glide */
    fun getVisualSource(): String = when {
        gifUrl.isNotBlank() -> gifUrl
        gifBase64.isNotBlank() -> gifBase64
        else -> ""
    }

    companion object {
        val DEFAULT_WORKOUTS: Map<String, List<WorkoutItem>> = mapOf(
            "lose_weight" to listOf(
                WorkoutItem(
                    id = "lw1", name = "Burpees",
                    description = "Full-body explosive movement combining squat, push-up and jump for maximum fat burn.",
                    gifUrl = "", sets = "3", reps = "15", caloriesBurn = 200,
                    goal = "lose_weight", category = "Cardio", durationMinutes = 20, difficulty = "Intermediate",
                    musclesTargeted = "Full Body,Core,Legs,Chest,Shoulders",
                    steps = "Stand with feet shoulder-width apart|Squat down and place hands on floor|Jump feet back into high plank|Perform one push-up|Jump feet back to hands|Explode upward jumping with arms overhead|Land softly and immediately repeat",
                    breathingTip = "Exhale forcefully as you jump up. Inhale as you lower. Short quick breaths — never hold breath during explosive moves.",
                    pointsToRemember = "Keep core braced the entire movement|Land softly with bent knees to protect joints|Modify by stepping out instead of jumping|Quality over speed — maintain form when fatigued",
                    equipment = "None", emoji = "💥", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "lw2", name = "Mountain Climbers",
                    description = "Core and cardio powerhouse that torches calories while building abs.",
                    gifUrl = "", sets = "3", reps = "20 each leg", caloriesBurn = 150,
                    goal = "lose_weight", category = "Abs", durationMinutes = 12, difficulty = "Beginner",
                    musclesTargeted = "Core,Hip Flexors,Shoulders,Chest,Triceps",
                    steps = "Start in high plank with hands under shoulders|Form straight line from head to heels|Drive right knee toward chest explosively|Return right foot and drive left knee in|Continue alternating in running motion|Keep hips level throughout",
                    breathingTip = "Exhale each time a knee drives forward. Short rhythmic breaths.",
                    pointsToRemember = "Hips must not rise or sag|Core fully braced every rep|Move as fast as form allows|Shoulders directly above wrists",
                    equipment = "None", emoji = "🧗", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "lw3", name = "High Knees",
                    description = "Elevate heart rate and activate hip flexors for maximum fat-burning.",
                    gifUrl = "", sets = "3", reps = "30 sec", caloriesBurn = 130,
                    goal = "lose_weight", category = "Cardio", durationMinutes = 10, difficulty = "Beginner",
                    musclesTargeted = "Hip Flexors,Quads,Core,Calves,Glutes",
                    steps = "Stand feet hip-width apart|Run in place lifting knees to hip height|Pump opposite arm with each knee drive|Keep pace fast and controlled|Maintain upright posture|Core engaged throughout",
                    breathingTip = "Quick rhythmic breaths matching movement pace. Breathe out each time a knee comes up.",
                    pointsToRemember = "Knees must reach at least waist level|Stay on balls of feet|Engage core|Torso upright with proud chest",
                    equipment = "None", emoji = "🏃", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "lw4", name = "Plank Hold",
                    description = "The gold-standard core endurance exercise activating every single muscle.",
                    gifUrl = "", sets = "3", reps = "45 sec", caloriesBurn = 80,
                    goal = "lose_weight", category = "Abs", durationMinutes = 10, difficulty = "Beginner",
                    musclesTargeted = "Core,Transverse Abdominis,Shoulders,Glutes,Lower Back",
                    steps = "Forearms on floor elbows under shoulders|Extend legs back on toes|Straight line head to heels|Squeeze glutes and brace core|Neck neutral looking at floor|Hold for full duration",
                    breathingTip = "Slow steady breathing. Inhale 3 seconds, exhale 3 seconds. NEVER hold breath.",
                    pointsToRemember = "Hips must not sag OR pike|Never hold breath during a hold|Squeezing glutes keeps hips level|Shaking is normal — muscles are working",
                    equipment = "None", emoji = "🪨", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "lw5", name = "Jump Squats",
                    description = "Explosive lower body movement for fat burn and leg power.",
                    gifUrl = "", sets = "3", reps = "15", caloriesBurn = 160,
                    goal = "lose_weight", category = "Legs", durationMinutes = 12, difficulty = "Intermediate",
                    musclesTargeted = "Quads,Glutes,Hamstrings,Calves,Core",
                    steps = "Stand feet shoulder-width|Lower into squat position|Explode upward jumping as high as possible|Land softly with knees bent|Absorb impact and immediately go into next squat|Keep chest up throughout",
                    breathingTip = "Exhale as you explode upward. Inhale as you land and lower into next rep.",
                    pointsToRemember = "Land toe-to-heel to protect knees|Knees never cave inward on landing|Start with regular squats if too difficult|Keep core tight throughout",
                    equipment = "None", emoji = "⬆️", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "lw6", name = "Bicycle Crunches",
                    description = "Scientifically proven most effective abs exercise for complete core definition.",
                    gifUrl = "", sets = "3", reps = "20 each side", caloriesBurn = 100,
                    goal = "lose_weight", category = "Abs", durationMinutes = 12, difficulty = "Beginner",
                    musclesTargeted = "Obliques,Rectus Abdominis,Transverse Abdominis,Hip Flexors",
                    steps = "Lie back feet off floor knees 90°|Hands lightly behind head|Lift shoulder blades off floor|Right knee toward chest extending left leg|Rotate left shoulder toward right knee|Switch sides in smooth pedaling motion|Lower back pressed into floor throughout",
                    breathingTip = "Exhale rotating and crunching. Inhale switching sides. Steady rhythm.",
                    pointsToRemember = "NEVER pull on neck|Move slowly with full control|Lower back flat on floor always|True torso rotation — not just elbow proximity",
                    equipment = "Exercise Mat", emoji = "🚴", isActive = true, createdAt = System.currentTimeMillis()
                )
            ),
            "gain_weight" to listOf(
                WorkoutItem(
                    id = "gw1", name = "Bench Press",
                    description = "The primary barbell compound movement for chest size and upper body strength.",
                    gifUrl = "", sets = "4", reps = "8", caloriesBurn = 160,
                    goal = "gain_weight", category = "Chest", durationMinutes = 25, difficulty = "Intermediate",
                    musclesTargeted = "Pectoralis Major,Triceps,Anterior Deltoids",
                    steps = "Lie flat on bench with eyes under bar|Plant feet flat on floor|Grip bar slightly wider than shoulders|Unrack and lower with control to mid-chest|Elbows at 45° from torso|Press bar straight up|Lock elbows without hyperextending",
                    breathingTip = "Inhale as you lower. Exhale forcefully as you press up. NEVER hold breath on the press.",
                    pointsToRemember = "Feet flat on floor always|Slight natural lower back arch|Do NOT bounce bar off chest|ALWAYS use a spotter for heavy sets|Retract shoulder blades into bench",
                    equipment = "Barbell, Flat Bench, Rack", emoji = "🏋️", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "gw2", name = "Deadlift",
                    description = "The most powerful full-body exercise for maximum muscle mass and strength.",
                    gifUrl = "", sets = "4", reps = "5", caloriesBurn = 200,
                    goal = "gain_weight", category = "Back", durationMinutes = 30, difficulty = "Advanced",
                    musclesTargeted = "Hamstrings,Glutes,Lower Back,Traps,Lats,Forearms,Quads",
                    steps = "Mid-foot under bar feet hip-width|Grip just outside legs|Bend knees until shins touch bar|Lift chest straighten back|Deep breath brace core maximally|Push floor away with legs|Drive hips forward as bar passes knees|Lock out hips fully at top|Lower in reverse with control",
                    breathingTip = "DEEP breath before pulling. Brace like taking a punch. HOLD through entire pull. Exhale only after lockout.",
                    pointsToRemember = "NEVER round lower back|Bar stays in contact with legs throughout|Push floor away|Start light and master form before loading",
                    equipment = "Barbell, Weight Plates", emoji = "⬆️", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "gw3", name = "Barbell Squat",
                    description = "The king of leg exercises for total lower body mass and strength.",
                    gifUrl = "", sets = "4", reps = "8", caloriesBurn = 180,
                    goal = "gain_weight", category = "Legs", durationMinutes = 25, difficulty = "Intermediate",
                    musclesTargeted = "Quads,Glutes,Hamstrings,Core,Adductors,Spinal Erectors",
                    steps = "Bar on rack at upper-chest height|Step under bar on upper traps|Grip wider than shoulders for stability|Unrack step back feet shoulder-width toes out 15-30°|Brace core hard — sit back and down chest up|Lower until thighs parallel to floor or below|Drive through whole foot back to top",
                    breathingTip = "Deep breath at top, brace and hold through descent. Exhale forcefully as you drive up past sticking point.",
                    pointsToRemember = "Knees track over toes — never cave inward|Weight through whole foot|Parallel depth minimum|Torso as upright as possible|Warm up extensively before heavy squats",
                    equipment = "Barbell, Squat Rack", emoji = "🦵", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "gw4", name = "Pull Ups",
                    description = "The definitive bodyweight exercise for a wide, powerful back.",
                    gifUrl = "", sets = "4", reps = "8", caloriesBurn = 140,
                    goal = "gain_weight", category = "Back", durationMinutes = 20, difficulty = "Intermediate",
                    musclesTargeted = "Latissimus Dorsi,Biceps,Rear Deltoids,Rhomboids,Teres Major",
                    steps = "Overhand grip wider than shoulders|Dead hang arms fully extended|Pull shoulder blades down and back|Drive elbows toward floor and back|Pull until chin clears bar|Pause briefly at top|Lower slowly with full control to dead hang",
                    breathingTip = "Exhale as you pull up. Inhale as you lower. Steady controlled breathing throughout.",
                    pointsToRemember = "No swinging or kipping — strict form only|Lead with chest not chin|Full range of motion|Use bands or machine if cannot do one yet",
                    equipment = "Pull Up Bar", emoji = "🔝", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "gw5", name = "Overhead Press",
                    description = "The fundamental shoulder mass builder and pressing strength standard.",
                    gifUrl = "", sets = "4", reps = "8", caloriesBurn = 150,
                    goal = "gain_weight", category = "Shoulders", durationMinutes = 22, difficulty = "Intermediate",
                    musclesTargeted = "Anterior Deltoids,Medial Deltoids,Triceps,Upper Trapezius,Serratus Anterior",
                    steps = "Stand feet shoulder-width slight knee bend|Barbell at upper chest overhand grip|Brace core and squeeze glutes|Press straight up tilting head back as bar passes face|Push head through at top|Lock elbows fully|Lower with full control to upper chest",
                    breathingTip = "Inhale and brace before each rep. Exhale forcefully as bar passes face going up. Re-brace every rep.",
                    pointsToRemember = "Tuck chin as bar passes|Never excessively arch lower back|Perfectly vertical bar path|Squeeze glutes for stability|Strict press — no leg drive",
                    equipment = "Barbell or Dumbbells", emoji = "🏆", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "gw6", name = "Tricep Dips",
                    description = "Compound pressing movement for thick powerful triceps and chest.",
                    gifUrl = "", sets = "4", reps = "10", caloriesBurn = 130,
                    goal = "gain_weight", category = "Arms", durationMinutes = 18, difficulty = "Intermediate",
                    musclesTargeted = "Triceps,Lower Chest,Front Deltoids,Pectorals",
                    steps = "Grip parallel bars palms inward|Lift yourself arms fully extended|Torso upright elbows close for triceps focus|Lower until upper arms parallel to floor|Never go below 90° to protect shoulders|Push back to full extension",
                    breathingTip = "Inhale lowering. Exhale pushing up. One complete breath per rep.",
                    pointsToRemember = "Don't dip too deep with shoulder issues|Control descent — never drop quickly|Add weight with belt when bodyweight is easy|Warm up shoulders thoroughly",
                    equipment = "Parallel Dip Bars", emoji = "💪", isActive = true, createdAt = System.currentTimeMillis()
                )
            ),
            "maintain" to listOf(
                WorkoutItem(
                    id = "mw1", name = "Push Ups",
                    description = "The timeless bodyweight classic for upper body strength and endurance.",
                    gifUrl = "", sets = "3", reps = "15", caloriesBurn = 120,
                    goal = "maintain", category = "Chest", durationMinutes = 15, difficulty = "Beginner",
                    musclesTargeted = "Pectoralis Major,Triceps,Anterior Deltoids,Core",
                    steps = "Hands slightly wider than shoulder-width|Extend legs back on toes|Rigid straight line head to heels|Lower chest elbows at 45°|Chest within one inch of floor|Push through palms back to start",
                    breathingTip = "Inhale as you lower. Exhale as you push up. 2 counts down, 1 count up.",
                    pointsToRemember = "Core braced — hips never sag or pike|Full range of motion gives 3× better results|Modify on knees if too difficult|Elevate feet for more chest focus",
                    equipment = "None", emoji = "👊", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "mw2", name = "Walking Lunges",
                    description = "Dynamic single-leg strength for balance, tone and functional stability.",
                    gifUrl = "", sets = "3", reps = "12 each leg", caloriesBurn = 110,
                    goal = "maintain", category = "Legs", durationMinutes = 14, difficulty = "Beginner",
                    musclesTargeted = "Quadriceps,Glutes,Hamstrings,Calves,Hip Flexors",
                    steps = "Stand tall feet together|Step forward right foot long stride|Lower back knee 1 inch from floor|Front thigh parallel to floor|Push through front heel|Repeat with left leg forward|Continue alternating walking forward",
                    breathingTip = "Inhale stepping forward and lowering. Exhale pushing back to standing.",
                    pointsToRemember = "Front knee over ankle — never past toes|Torso perfectly upright|Long enough step for 90° angles|Control the lowering phase",
                    equipment = "None", emoji = "🚶", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "mw3", name = "Dumbbell Rows",
                    description = "Essential back exercise for posture correction and upper body balance.",
                    gifUrl = "", sets = "3", reps = "12 each side", caloriesBurn = 130,
                    goal = "maintain", category = "Back", durationMinutes = 15, difficulty = "Beginner",
                    musclesTargeted = "Latissimus Dorsi,Rhomboids,Rear Deltoids,Biceps,Teres Major",
                    steps = "Right hand and knee on bench for support|Dumbbell in left hand hanging down|Back completely flat parallel to floor|Pull dumbbell straight up to hip|Drive elbow back as if elbowing behind you|Squeeze shoulder blade hard at top|Lower with full control|Switch sides and repeat",
                    breathingTip = "Exhale rowing up. Inhale lowering. Steady controlled breathing throughout set.",
                    pointsToRemember = "Do NOT rotate torso|Pull with elbow not hand|Full stretch at bottom every rep|Back flat — never round",
                    equipment = "Dumbbell, Flat Bench", emoji = "🏋️", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "mw4", name = "Glute Bridges",
                    description = "Target glutes and hamstrings while strengthening lower back and core stability.",
                    gifUrl = "", sets = "3", reps = "20", caloriesBurn = 90,
                    goal = "maintain", category = "Legs", durationMinutes = 12, difficulty = "Beginner",
                    musclesTargeted = "Glutes,Hamstrings,Core,Lower Back,Hip Flexors",
                    steps = "Lie back knees bent feet flat on floor|Feet hip-width close enough to touch heels|Arms flat at sides palms down|Push through heels squeeze glutes|Lift hips — straight line shoulder to knee|Hold 2 seconds squeeze maximally|Lower slowly with control|Repeat without letting hips touch ground between reps",
                    breathingTip = "Exhale pushing hips up. Inhale lowering. Brief breath hold at top intensifies the squeeze.",
                    pointsToRemember = "Drive through heels not toes|Squeeze glutes as hard as possible at top|Don't hyperextend lower back at peak|Add resistance band above knees for extra activation",
                    equipment = "Exercise Mat", emoji = "🍑", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "mw5", name = "Resistance Band Curls",
                    description = "Constant-tension curl for arm toning, strength and muscle definition.",
                    gifUrl = "", sets = "3", reps = "15", caloriesBurn = 100,
                    goal = "maintain", category = "Arms", durationMinutes = 12, difficulty = "Beginner",
                    musclesTargeted = "Biceps Brachii,Brachialis,Brachioradialis,Forearms",
                    steps = "Stand on middle of band feet shoulder-width|Handle in each hand palms forward|Pin elbows firmly at sides|Curl both handles up toward shoulders|Squeeze biceps hard at peak — 1 second hold|Lower slowly in 3 full counts|Repeat without swinging",
                    breathingTip = "Exhale curling up. Inhale lowering. The lowering phase should be twice as slow as the lift.",
                    pointsToRemember = "Elbows fixed at sides — they are the pivot|No body swing or momentum|Slow lowering builds as much muscle as lifting|Squeeze hard at peak for maximum contraction",
                    equipment = "Resistance Band", emoji = "💪", isActive = true, createdAt = System.currentTimeMillis()
                ),
                WorkoutItem(
                    id = "mw6", name = "Plank Shoulder Taps",
                    description = "Advanced plank variation for core stability and anti-rotation strength.",
                    gifUrl = "", sets = "3", reps = "20 total taps", caloriesBurn = 95,
                    goal = "maintain", category = "Abs", durationMinutes = 10, difficulty = "Beginner",
                    musclesTargeted = "Core,Shoulders,Triceps,Obliques,Transverse Abdominis",
                    steps = "Start in high plank hands directly under shoulders|Feet slightly wider than hip-width for stability|Brace core and squeeze glutes hard|Lift right hand and tap left shoulder|Return right hand to floor|Lift left hand and tap right shoulder|Keep hips completely still throughout — zero rotation",
                    breathingTip = "Breathe steadily throughout. Exhale as you tap. Never hold your breath.",
                    pointsToRemember = "Hips must stay completely square — no rocking|Wider foot stance helps with stability|The challenge is preventing hip rotation|Slow is harder; speed defeats the purpose",
                    equipment = "None", emoji = "👐", isActive = true, createdAt = System.currentTimeMillis()
                )
            )
        )
    }
}