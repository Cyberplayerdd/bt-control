package com.btcontrol.app

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/** Localized text, usable inside and outside composables. */
fun t(@StringRes id: Int): String = BtControlApp.instance.getString(id)

/** Localized text with arguments (%1\$s, %1\$d…). */
fun t(@StringRes id: Int, vararg args: Any): String = BtControlApp.instance.getString(id, *args)

/** Localized text for a count ("1 step" / "2 steps", with Russian, Polish… plural forms). */
fun tq(@PluralsRes id: Int, count: Int): String =
    BtControlApp.instance.resources.getQuantityString(id, count, count)
