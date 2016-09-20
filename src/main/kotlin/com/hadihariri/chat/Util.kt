package com.hadihariri.chat

infix fun String.equalsIgnoreCase(other: String) = equals(other, true)