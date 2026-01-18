package com.xortroll.emuiibo.emuiigen

import org.json.JSONObject

data class AmiiboAreaEntry(val program_id: ULong, val access_id: UInt, val is_active: Boolean) {
    companion object {
        fun fromJson(json: JSONObject) : AmiiboAreaEntry {
            val program_id = json.getLong("program_id").toULong();
            val access_id = json.getInt("access_id").toUInt();
            val is_active = json.optBoolean("is_active", false);
            return AmiiboAreaEntry(program_id, access_id, is_active);
        }
    }

    fun toJson() : JSONObject {
        val json_obj = JSONObject();
        json_obj.put("program_id", this.program_id);
        json_obj.put("access_id", this.access_id);
        json_obj.put("is_active", this.is_active);
        return json_obj;
    }
}