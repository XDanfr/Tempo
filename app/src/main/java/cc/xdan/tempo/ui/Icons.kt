package cc.xdan.tempo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import cc.xdan.tempo.model.SubjectIcon

fun SubjectIcon.vector(): ImageVector? = when (this) {
    SubjectIcon.NONE -> null
    SubjectIcon.CODE -> Icons.Outlined.Code
    SubjectIcon.SCIENCE -> Icons.Outlined.Science
    SubjectIcon.MUSIC -> Icons.Outlined.MusicNote
    SubjectIcon.PERSON -> Icons.Outlined.Person
    SubjectIcon.WORK -> Icons.Outlined.WorkOutline
    SubjectIcon.BOOK -> Icons.Outlined.MenuBook
    SubjectIcon.MATHS -> Icons.Outlined.Calculate
    SubjectIcon.ART -> Icons.Outlined.Palette
    SubjectIcon.HISTORY -> Icons.Outlined.HistoryEdu
    SubjectIcon.LANGUAGES -> Icons.Outlined.Translate
    SubjectIcon.GEOGRAPHY -> Icons.Outlined.Public
    SubjectIcon.BIOLOGY -> Icons.Outlined.Biotech
    SubjectIcon.PHYSICS -> Icons.Outlined.Bolt
    SubjectIcon.LITERATURE -> Icons.Outlined.AutoStories
    SubjectIcon.WRITING -> Icons.Outlined.EditNote
    SubjectIcon.ECONOMICS -> Icons.Outlined.TrendingUp
    SubjectIcon.BUSINESS -> Icons.Outlined.BusinessCenter
    SubjectIcon.LAW -> Icons.Outlined.Gavel
    SubjectIcon.MEDICINE -> Icons.Outlined.MedicalServices
    SubjectIcon.PSYCHOLOGY -> Icons.Outlined.Psychology
    SubjectIcon.SPORT -> Icons.Outlined.SportsSoccer
    SubjectIcon.FITNESS -> Icons.Outlined.FitnessCenter
    SubjectIcon.DANCE -> Icons.Outlined.DirectionsWalk
    SubjectIcon.DRAMA -> Icons.Outlined.TheaterComedy
    SubjectIcon.FILM -> Icons.Outlined.Movie
    SubjectIcon.PHOTOGRAPHY -> Icons.Outlined.PhotoCamera
    SubjectIcon.DESIGN -> Icons.Outlined.DesignServices
    SubjectIcon.ENGINEERING -> Icons.Outlined.Engineering
    SubjectIcon.ROBOTICS -> Icons.Outlined.SmartToy
    SubjectIcon.ELECTRONICS -> Icons.Outlined.Memory
    SubjectIcon.GAMING -> Icons.Outlined.SportsEsports
    SubjectIcon.NETWORKING -> Icons.Outlined.Lan
    SubjectIcon.SECURITY -> Icons.Outlined.Security
    SubjectIcon.TRAVEL -> Icons.Outlined.Commute
    SubjectIcon.CAR -> Icons.Outlined.DirectionsCar
    SubjectIcon.BUS -> Icons.Outlined.DirectionsBus
    SubjectIcon.BIKE -> Icons.Outlined.DirectionsBike
    SubjectIcon.WALK -> Icons.Outlined.DirectionsWalk
    SubjectIcon.COFFEE -> Icons.Outlined.LocalCafe
    SubjectIcon.LUNCH -> Icons.Outlined.Restaurant
    SubjectIcon.BREAK -> Icons.Outlined.PauseCircleOutline
    SubjectIcon.CHANGEOVER -> Icons.Outlined.SwapHoriz
    SubjectIcon.REST -> Icons.Outlined.Spa
    SubjectIcon.SHOPPING -> Icons.Outlined.ShoppingBag
    SubjectIcon.VOLUNTEER -> Icons.Outlined.VolunteerActivism
    SubjectIcon.MEETING -> Icons.Outlined.Groups
    SubjectIcon.CALL -> Icons.Outlined.Call
    SubjectIcon.EMAIL -> Icons.Outlined.MailOutline
    SubjectIcon.HOME -> Icons.Outlined.Home
    SubjectIcon.OFFICE -> Icons.Outlined.Apartment
    SubjectIcon.LIBRARY -> Icons.Outlined.LocalLibrary
    SubjectIcon.CALENDAR -> Icons.Outlined.EventNote
    SubjectIcon.TIME -> Icons.Outlined.Schedule
    SubjectIcon.STAR -> Icons.Outlined.StarOutline
    SubjectIcon.HEART -> Icons.Outlined.FavoriteBorder
    SubjectIcon.IDEA -> Icons.Outlined.Lightbulb
    SubjectIcon.CHECK -> Icons.Outlined.CheckCircleOutline
    SubjectIcon.FOLDER -> Icons.Outlined.FolderOpen
    SubjectIcon.TOOLS -> Icons.Outlined.Build
    SubjectIcon.NATURE -> Icons.Outlined.Park
    SubjectIcon.PETS -> Icons.Outlined.Pets
    SubjectIcon.ACCESSIBILITY -> Icons.Outlined.Accessible
}

data class IconOption(val icon: SubjectIcon, val title: String, val category: String, val keywords: String)
val iconOptions = listOf(
    IconOption(SubjectIcon.NONE, "None", "Everyday", "no icon"),
    IconOption(SubjectIcon.CODE, "Computer science", "Study", "computing programming coding"),
    IconOption(SubjectIcon.SCIENCE, "Science", "Study", "chemistry laboratory"),
    IconOption(SubjectIcon.MUSIC, "Music", "Creative", "production sound audio"),
    IconOption(SubjectIcon.PERSON, "Tutor", "Study", "personal tutorial"),
    IconOption(SubjectIcon.WORK, "Work", "Work", "job"),
    IconOption(SubjectIcon.BOOK, "Reading", "Study", "literature book"),
    IconOption(SubjectIcon.MATHS, "Maths", "Study", "mathematics numbers calculation"),
    IconOption(SubjectIcon.ART, "Art", "Creative", "painting drawing colour"),
    IconOption(SubjectIcon.HISTORY, "History", "Study", "past"),
    IconOption(SubjectIcon.LANGUAGES, "Languages", "Study", "english french spanish german"),
    IconOption(SubjectIcon.GEOGRAPHY, "Geography", "Study", "earth world"),
    IconOption(SubjectIcon.BIOLOGY, "Biology", "Study", "life"),
    IconOption(SubjectIcon.PHYSICS, "Physics", "Study", "electricity energy"),
    IconOption(SubjectIcon.LITERATURE, "Literature", "Study", "english reading"),
    IconOption(SubjectIcon.WRITING, "Writing", "Creative", "notes essay"),
    IconOption(SubjectIcon.ECONOMICS, "Economics", "Study", "finance"),
    IconOption(SubjectIcon.BUSINESS, "Business", "Work", "business studies"),
    IconOption(SubjectIcon.LAW, "Law", "Study", "legal"),
    IconOption(SubjectIcon.MEDICINE, "Medicine", "Study", "health nursing"),
    IconOption(SubjectIcon.PSYCHOLOGY, "Psychology", "Study", "mind"),
    IconOption(SubjectIcon.SPORT, "Sport", "Everyday", "pe football"),
    IconOption(SubjectIcon.FITNESS, "Fitness", "Everyday", "gym exercise"),
    IconOption(SubjectIcon.DANCE, "Dance", "Creative", "movement"),
    IconOption(SubjectIcon.DRAMA, "Drama", "Creative", "theatre performance"),
    IconOption(SubjectIcon.FILM, "Film", "Creative", "cinema video editing"),
    IconOption(SubjectIcon.PHOTOGRAPHY, "Photography", "Creative", "camera"),
    IconOption(SubjectIcon.DESIGN, "Design", "Creative", "graphics"),
    IconOption(SubjectIcon.ENGINEERING, "Engineering", "Study", "construction"),
    IconOption(SubjectIcon.ROBOTICS, "Robotics", "Study", "robot"),
    IconOption(SubjectIcon.ELECTRONICS, "Electronics", "Study", "hardware chip"),
    IconOption(SubjectIcon.GAMING, "Gaming", "Everyday", "games"),
    IconOption(SubjectIcon.NETWORKING, "Networking", "Study", "internet"),
    IconOption(SubjectIcon.SECURITY, "Security", "Work", "cybersecurity"),
    IconOption(SubjectIcon.TRAVEL, "Travel", "Everyday", "transport"),
    IconOption(SubjectIcon.CAR, "Car", "Everyday", "drive"),
    IconOption(SubjectIcon.BUS, "Bus", "Everyday", "transport"),
    IconOption(SubjectIcon.BIKE, "Bike", "Everyday", "cycle cycling"),
    IconOption(SubjectIcon.WALK, "Walk", "Everyday", "walking"),
    IconOption(SubjectIcon.COFFEE, "Coffee", "Everyday", "drink"),
    IconOption(SubjectIcon.LUNCH, "Lunch", "Everyday", "food meal dinner"),
    IconOption(SubjectIcon.BREAK, "Break", "Everyday", "pause"),
    IconOption(SubjectIcon.CHANGEOVER, "Changeover", "Everyday", "transition"),
    IconOption(SubjectIcon.REST, "Rest", "Everyday", "relax wellness"),
    IconOption(SubjectIcon.SHOPPING, "Shopping", "Everyday", "buy"),
    IconOption(SubjectIcon.VOLUNTEER, "Volunteering", "Work", "community"),
    IconOption(SubjectIcon.MEETING, "Meeting", "Work", "team group"),
    IconOption(SubjectIcon.CALL, "Call", "Work", "phone"),
    IconOption(SubjectIcon.EMAIL, "Email", "Work", "mail message"),
    IconOption(SubjectIcon.HOME, "Home", "Everyday", "house"),
    IconOption(SubjectIcon.OFFICE, "Office", "Work", "building"),
    IconOption(SubjectIcon.LIBRARY, "Library", "Study", "study books"),
    IconOption(SubjectIcon.CALENDAR, "Calendar", "Everyday", "event"),
    IconOption(SubjectIcon.TIME, "Time", "Everyday", "clock"),
    IconOption(SubjectIcon.STAR, "Star", "Everyday", "favourite"),
    IconOption(SubjectIcon.HEART, "Heart", "Everyday", "love"),
    IconOption(SubjectIcon.IDEA, "Idea", "Creative", "light inspiration"),
    IconOption(SubjectIcon.CHECK, "Check", "Work", "task done"),
    IconOption(SubjectIcon.FOLDER, "Folder", "Work", "files"),
    IconOption(SubjectIcon.TOOLS, "Tools", "Work", "repair"),
    IconOption(SubjectIcon.NATURE, "Nature", "Everyday", "trees environment"),
    IconOption(SubjectIcon.PETS, "Pets", "Everyday", "animals"),
    IconOption(SubjectIcon.ACCESSIBILITY, "Accessibility", "Everyday", "access"),
)
fun SubjectIcon.label(): String = iconOptions.first { it.icon == this }.title
val weekdayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
