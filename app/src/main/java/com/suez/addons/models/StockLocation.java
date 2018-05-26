package com.suez.addons.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-17.
 */

public class StockLocation extends OModel {
    private static final String TAG = StockLocation.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_name, OVarchar.class).setSize(64);
    OColumn usage = new OColumn(getContext(), R.string.column_location_usage, OVarchar.class);
    OColumn is_int = new OColumn(getContext(), R.string.column_is_int_location, OBoolean.class);
    OColumn is_pretreatment = new OColumn(getContext(), R.string.column_is_pretrement, OBoolean.class);

    public StockLocation(Context context, OUser user) {
        super(context, "stock.location", user);
    }
}
