package com.serratocreations.phovo.core.database.converters

import androidx.room.TypeConverter
import com.serratocreations.phovo.core.database.entities.AssetLocation

class AssetLocationConverter {
    @TypeConverter
    fun fromSerialId(value: Int): AssetLocation {
        return AssetLocation.getFromSerialId(value)
    }

    @TypeConverter
    fun toSerialId(assetLocation: AssetLocation): Int {
        return assetLocation.serialId
    }
}