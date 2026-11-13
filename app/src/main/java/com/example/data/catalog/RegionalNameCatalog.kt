package com.example.data.catalog

import com.example.data.model.AppConfig
import com.example.data.model.CoinPackage
import com.example.data.model.CuratedRegionalName

object RegionalNameCatalog {

  val regions: List<String> = listOf(
    "All Regions",
    "Punjabi",
    "Sindhi",
    "Pashtun",
    "Baloch",
    "Kashmiri",
    "Gilgit-Baltistani",
    "Urdu / Muhajir",
    "Arabic & Islamic",
    "Persian & Turkic",
  )

  val genders: List<String> = listOf("All", "Boy", "Girl", "Unisex")

  val coinPackages: List<CoinPackage> = listOf(
    CoinPackage("pkg_mini", "50 Giveaway Coins", 50L, "PKR 100", "5 Tickets"),
    CoinPackage("pkg_starter", "Starter Pouch", 100L, "PKR 200", "Popular"),
    CoinPackage("pkg_value", "Heritage Bundle", 300L, "PKR 500", "Best Value"),
    CoinPackage("pkg_champion", "Crescent Chest", 750L, "PKR 1,100", "+15% Bonus"),
    CoinPackage("pkg_patron", "Community Patron", 1500L, "PKR 2,000", "Top Supporter"),
  )

  val defaultAppConfig = AppConfig(
    id = "config",
    whatsappGroupUrl = "https://chat.whatsapp.com/PakGivesCoinsOfficial",
    whatsappAdminContact = "+92 300 0000000 • PakGives Coin Desk",
    termsTitle = "PakGives Terms, Policies & Community Guidelines",
    termsBody = """
      1. Community & Multiuser Respect
      PakGives is an online multiuser platform built for sharing Pakistani and regional name heritage, participating in community giveaways, and supporting verified charitable causes. Users must keep submitted names, cultural notes, and donation messages respectful and authentic.

      2. Giveaway Coins & WhatsApp Group Purchases
      Giveaway Coins are virtual community tokens used inside PakGives to enter giveaways and support donation drives. Coins can be purchased through the official PakGives WhatsApp group listed in the Buy Coins section. After coordinating payment with a verified group admin, submit your WhatsApp reference code in the app for manual approval and balance credit.

      3. Donations & Transparency (No Tickets)
      Every Giveaway Coin donated to a campaign is pure charity — donations do not generate giveaway tickets. Users may donate 1 to unlimited coins from their wallet, recorded transparently in personal receipts alongside verified admin Helping Donations and Payout Proofs.

      4. Privacy & Google Account Data
      Your Google Sign-In profile (display name, UID, and email) is used strictly for authentication, wallet security, and giveaway winner attribution. Email addresses are never exposed publicly to other users.
    """.trimIndent(),
    giveawayPolicyBody = """
      • Fixed Ticket Price: Every giveaway ticket costs 10 Giveaway Coins.
      • Unlimited Ticket Purchases: Users may buy 1 to infinity tickets in a single purchase (subject to wallet balance and any admin-configured ticket limit).
      • Weighted Fair Draw: Each purchased ticket enters the pool; when an admin draws a winner, odds are proportional to verified ticket count.
      • Coin Finality & Support: Coins spent on tickets or donations are deducted atomically. Use Report a Problem in the More tab anytime for admin support.
    """.trimIndent(),
  )

  val curatedNames: List<CuratedRegionalName> = listOf(
    // Punjabi
    CuratedRegionalName("cur_pun_1", "Waris", "وارث", "Waa-ris", "Punjabi", "Boy", "Heir, protector, inheritor of noble legacy", "Honors Sufi poet Waris Shah of Punjab."),
    CuratedRegionalName("cur_pun_2", "Heer", "ہیر", "Heer", "Punjabi", "Girl", "Diamond, pure spirit, timeless strength", "Iconic Punjabi literary heritage name."),
    CuratedRegionalName("cur_pun_3", "Sarmad", "سرمد", "Sar-mad", "Punjabi", "Boy", "Eternal, everlasting truth and serenity", "Rooted in classical Punjabi Sufi verse."),
    CuratedRegionalName("cur_pun_4", "Chanab", "چناب", "Cha-naab", "Punjabi", "Unisex", "Moonlit river, flowing grace", "Named after the beloved Chenab River of Punjab."),
    CuratedRegionalName("cur_pun_5", "Mahin", "ماہین", "Maa-heen", "Punjabi", "Girl", "Fine, delicate, luminous like moonlight", "Warm melodic favorite across Lahore and Multan."),
    CuratedRegionalName("cur_pun_6", "Shahbaz", "شہباز", "Shah-baaz", "Punjabi", "Boy", "Royal falcon, fearless visionary", "Associated with Lal Shahbaz and Punjabi valor."),

    // Sindhi
    CuratedRegionalName("cur_sin_1", "Latif", "لطیف", "La-teef", "Sindhi", "Boy", "Gentle, subtle, compassionate wisdom", "Inspired by Shah Abdul Latif Bhittai of Sindh."),
    CuratedRegionalName("cur_sin_2", "Marvi", "ماروی", "Maar-vee", "Sindhi", "Girl", "Symbol of loyalty, homeland love, and dignity", "Celebrated heroine of Thar and Sindhi folklore."),
    CuratedRegionalName("cur_sin_3", "Sachal", "سچل", "Sa-chal", "Sindhi", "Boy", "Truthful, sincere seeker", "Honors Sufi poet Sachal Sarmast."),
    CuratedRegionalName("cur_sin_4", "Sassui", "سسئي", "Sas-su-ee", "Sindhi", "Girl", "Moonlight, steadfast devotion", "Classic Sindhi poetic heritage name."),
    CuratedRegionalName("cur_sin_5", "Mehran", "مہران", "Meh-raan", "Sindhi", "Unisex", "Ancient name of the Indus River; generous", "Deeply tied to Sindh's river civilization."),
    CuratedRegionalName("cur_sin_6", "Suhni", "سوہنی", "Soh-nee", "Sindhi", "Girl", "Graceful, radiant, beautiful soul", "From timeless Sindhi & Punjabi folk heritage."),

    // Pashtun
    CuratedRegionalName("cur_pas_1", "Khushal", "خوشحال", "Khu-shaal", "Pashtun", "Boy", "Prosperous, joyful, resilient leader", "Honors Pashto warrior-poet Khushal Khan Khattak."),
    CuratedRegionalName("cur_pas_2", "Malala", "ملاله", "Ma-laa-la", "Pashtun", "Girl", "Graceful courage, tender-hearted strength", "Named for Malalai of Maiwand, Pashtun heroine."),
    CuratedRegionalName("cur_pas_3", "Zaryab", "زریاب", "Zar-yaab", "Pashtun", "Boy", "Gilded with gold, prosperous, bright", "Beloved across Peshawar, Swat, and Quetta."),
    CuratedRegionalName("cur_pas_4", "Spogmay", "سپوږمۍ", "Spog-may", "Pashtun", "Girl", "Full moon, radiant night light", "Pure Pashto poetic nature name."),
    CuratedRegionalName("cur_pas_5", "Atal", "اتل", "A-tal", "Pashtun", "Boy", "Hero, champion, unwavering protector", "Traditional Pashto name symbolizing bravery."),
    CuratedRegionalName("cur_pas_6", "Wagma", "وږمه", "Wag-ma", "Pashtun", "Girl", "Morning mountain breeze, gentle fragrance", "Evokes cool breezes of the Khyber valleys."),

    // Baloch
    CuratedRegionalName("cur_bal_1", "Chakar", "چاکر", "Chaa-kar", "Baloch", "Boy", "Noble chieftain, courageous guardian", "Honors Mir Chakar Khan Rind of Balochistan."),
    CuratedRegionalName("cur_bal_2", "Hani", "ہانی", "Haa-nee", "Baloch", "Girl", "Gracious, joyful, noble-hearted", "Famous from the Balochi epic of Hani & Sheh Mureed."),
    CuratedRegionalName("cur_bal_3", "Mureed", "مرید", "Mu-reed", "Baloch", "Boy", "Devoted, loyal, steadfast spirit", "Classical Balochi literary heritage."),
    CuratedRegionalName("cur_bal_4", "Banadi", "بنادی", "Ba-naa-dee", "Baloch", "Girl", "Respected lady, pearl of the home", "Traditional honorific name in Makran & Kalat."),
    CuratedRegionalName("cur_bal_5", "Hammil", "حمل", "Ham-mil", "Baloch", "Boy", "Fearless mariner, protector of the coast", "Honors 16th-century Balochi hero Hammal Jiand."),
    CuratedRegionalName("cur_bal_6", "Sammoh", "سموہ", "Sam-moh", "Baloch", "Girl", "Bright star, cherished jewel", "Warm regional favorite in Balochistan."),

    // Kashmiri
    CuratedRegionalName("cur_kas_1", "Zooni", "زونی", "Zoo-nee", "Kashmiri", "Girl", "Moonlight, poetic radiance", "The original name of Kashmir's poetess Habba Khatoon."),
    CuratedRegionalName("cur_kas_2", "Chinar", "چنار", "Chi-naar", "Kashmiri", "Unisex", "Majestic plane tree, enduring shelter", "Icon of Kashmir's autumn golden valleys."),
    CuratedRegionalName("cur_kas_3", "Mehjoor", "مہجور", "Meh-joor", "Kashmiri", "Boy", "Eloquent voice, awakener of hearts", "Honors beloved Kashmiri poet Ghulam Ahmad Mahjoor."),
    CuratedRegionalName("cur_kas_4", "Arni", "ارنی", "Ar-nee", "Kashmiri", "Girl", "Wild golden rose of the mountains", "From Kashmiri alpine botanical tradition."),
    CuratedRegionalName("cur_kas_5", "Sheeraz", "شیراز", "Shee-raaz", "Kashmiri", "Boy", "Sweetness, refined culture and wisdom", "Deeply cherished in Kashmiri households."),
    CuratedRegionalName("cur_kas_6", "Noorie", "نوری", "Noo-ree", "Kashmiri", "Girl", "Radiant light, serene beauty", "Celebrated in Neelam Valley folklore."),

    // Gilgit-Baltistani
    CuratedRegionalName("cur_gb_1", "Rakaposhi", "راکاپوشی", "Raa-kaa-po-shee", "Gilgit-Baltistani", "Girl", "Snow-covered peak, serene majesty", "Inspired by Nagar Valley's iconic white mountain."),
    CuratedRegionalName("cur_gb_2", "Sherbaz", "شیرباز", "Sher-baaz", "Gilgit-Baltistani", "Boy", "Lion-hearted falcon of the peaks", "Commonly revered across Hunza and Gilgit."),
    CuratedRegionalName("cur_gb_3", "Shandur", "شندور", "Shan-door", "Gilgit-Baltistani", "Unisex", "High plateau of unity and courage", "Named for the roof of the world between Chitral and Gilgit."),
    CuratedRegionalName("cur_gb_4", "Gulmit", "گلمت", "Gul-mit", "Gilgit-Baltistani", "Girl", "Valley of blossoms", "Inspired by upper Hunza's spring orchards."),
    CuratedRegionalName("cur_gb_5", "Skardu", "سکردو", "Skar-du", "Gilgit-Baltistani", "Boy", "Starry highland fortress", "Balti heritage name from the Karakoram."),
    CuratedRegionalName("cur_gb_6", "Batura", "بتورہ", "Ba-too-ra", "Gilgit-Baltistani", "Girl", "Pure glacial brilliance", "Honors the majestic Batura peaks."),

    // Urdu / Muhajir
    CuratedRegionalName("cur_urd_1", "Arham", "ارحم", "Ar-ham", "Urdu / Muhajir", "Boy", "Most compassionate, merciful heart", "Beloved modern classic across Karachi & Islamabad."),
    CuratedRegionalName("cur_urd_2", "Inaya", "عنایہ", "I-naa-ya", "Urdu / Muhajir", "Girl", "Care, divine blessing, gentle protection", "Poetic and widely cherished Urdu choice."),
    CuratedRegionalName("cur_urd_3", "Zavian", "زاویان", "Zaa-vi-yaan", "Urdu / Muhajir", "Boy", "Bright, luminous, forward-looking", "Contemporary Urdu name with warm cadence."),
    CuratedRegionalName("cur_urd_4", "Aiza", "عائزہ", "Aai-za", "Urdu / Muhajir", "Girl", "Noble, respected, dignified grace", "Elegant and concise Urdu heritage favorite."),
    CuratedRegionalName("cur_urd_5", "Hamdan", "حمدان", "Ham-daan", "Urdu / Muhajir", "Boy", "One who praises, grateful soul", "Classic Urdu literary warmth."),
    CuratedRegionalName("cur_urd_6", "Meher", "مہر", "Me-her", "Urdu / Muhajir", "Unisex", "Kindness, sun, affectionate grace", "Versatile name rooted in Urdu poetry."),

    // Arabic & Islamic
    CuratedRegionalName("cur_arb_1", "Rayyan", "ریّان", "Ray-yaan", "Arabic & Islamic", "Boy", "Lush, watered, gate of paradise", "Timeless Quranic and classical Arabic favorite."),
    CuratedRegionalName("cur_arb_2", "Amina", "آمنہ", "Aa-mi-na", "Arabic & Islamic", "Girl", "Trustworthy, peaceful, serene", "Deeply honored across Islamic history."),
    CuratedRegionalName("cur_arb_3", "Zayd", "زید", "Zayd", "Arabic & Islamic", "Boy", "Abundance, growth, generosity", "Short, resonant classical Arabic name."),
    CuratedRegionalName("cur_arb_4", "Maryam", "مریم", "Mar-yam", "Arabic & Islamic", "Girl", "Devoted, exalted, sea of purity", "One of the most revered Quranic names."),
    CuratedRegionalName("cur_arb_5", "Ibrahim", "ابراہیم", "Ib-raa-heem", "Arabic & Islamic", "Boy", "Father of multitudes, steadfast friend", "Prophetic heritage name."),
    CuratedRegionalName("cur_arb_6", "Noor", "نور", "Noor", "Arabic & Islamic", "Unisex", "Divine light, clarity, illumination", "Universal beacon of warmth and peace."),

    // Persian & Turkic
    CuratedRegionalName("cur_per_1", "Darius", "داریوش", "Daa-ri-ush", "Persian & Turkic", "Boy", "Upholder of good, regal wisdom", "Ancient Persian heritage admired across South Asia."),
    CuratedRegionalName("cur_per_2", "Parisa", "پریسا", "Pa-ree-sa", "Persian & Turkic", "Girl", "Graceful, angelic serenity", "Melodic Persian classic."),
    CuratedRegionalName("cur_per_3", "Taimur", "تیمور", "Tai-moor", "Persian & Turkic", "Boy", "Iron-willed, resilient strength", "Central Asian Turkic heritage."),
    CuratedRegionalName("cur_per_4", "Laleh", "لالہ", "Laa-leh", "Persian & Turkic", "Girl", "Mountain tulip, vibrant spring", "Celebrated in Iqbal's Persian & Urdu poetry."),
    CuratedRegionalName("cur_per_5", "Kaveh", "کاوہ", "Kaa-veh", "Persian & Turkic", "Boy", "Courageous artisan, champion of justice", "Epic Persian literary heritage."),
    CuratedRegionalName("cur_per_6", "Shirin", "شیریں", "Shee-reen", "Persian & Turkic", "Girl", "Sweet, gentle, eloquent", "Timeless Persian poetic emblem."),
  )
}
