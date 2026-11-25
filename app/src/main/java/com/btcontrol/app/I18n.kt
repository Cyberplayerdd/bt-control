package com.btcontrol.app

import androidx.annotation.StringRes

/** Localized text, usable inside and outside composables. */
fun t(@StringRes id: Int): String = BtControlApp.instance.getString(id)

/** Localized text with arguments (%1\$s, %1\$d…). */
fun t(@StringRes id: Int, vararg args: Any): String = BtControlApp.instance.getString(id, *args)
