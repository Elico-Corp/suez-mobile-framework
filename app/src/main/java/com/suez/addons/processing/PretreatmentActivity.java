package com.suez.addons.processing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.utils.ODateUtils;
import com.suez.SuezConstants;
import com.suez.utils.CallMethodsOnlineUtils;
import com.suez.utils.RecordUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by joseph on 18-5-17.
 */

public class PretreatmentActivity extends ProcessingActivity {
    private static final String TAG = PretreatmentActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar(R.string.label_pretreatment);
    }

    @Override
    protected void initView() {
        super.initView();
        pretreatmentLocation.setVisibility(View.VISIBLE);
        destinationLocation.setVisibility(View.VISIBLE);
        remainQty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("pretreatment_location_id", 0);
        wizardValues.put("destination_location_id", 0);
        wizardValues.put("pretreatment_type_id", 0);
        wizardValues.put("qty", 0.0f);
        wizardValues.put("remain_qty", 0.0f);
        wizardValues.put("action", SuezConstants.PRETREATMENT_KEY);
        wizardValues.put("prodlot_id", prodlot_id);
        super.initForm();
    }

    @Override
    protected void performProcessing() {
        OValues inputValues = pretreatmentWizardForm.getValues();
        Float quantity = records.get(0).getFloat("input_qty");
        Float remainQuantity = Float.parseFloat(remainQty.getValue().toString());
        if (isNetwork) {
            OArguments args = new OArguments();
            args.add(new JSONArray().put(prodlot_id));
            HashMap<String, Object> kwargs = new HashMap<>();
            kwargs.put("lot_id", prodlot_id);
            kwargs.put("quantity", quantity);
            kwargs.put("quant_id", quant_id);
            kwargs.put("pretreatment_location_id", stockLocation.browse(inputValues.getInt("pretreatment_location_id")).getInt("id"));
            kwargs.put("location_dest_id", stockLocation.browse(inputValues.getInt("destination_location_id")).getInt("id"));
            HashMap<String, Object> map = new HashMap<>();
            map.put("data", kwargs);
            map.put("action", SuezConstants.PRETREATMENT_KEY);
            map.put("action_uid", UUID.randomUUID().toString());
            BaseAbstractListener listener = new BaseAbstractListener() {
                @Override
                public void OnSuccessful(Object obj) {
                    postProcessing(obj);
                }
            };
            CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data", new OArguments(), null, map)
                    .setListener(listener);
            utils.callMethodOnServer();
        } else {
            ODataRow prodlot = stockProductionLot.browse(prodlot_id);
            OValues lotValues = prodlot.toValues();
            lotValues.removeKey("_id");
            lotValues.removeKey("id");
            lotValues.removeKey("quant_ids");
            lotValues.put("product_qty", quantity);
            lotValues.put("name", ODateUtils.getDate("yyMMdd") + stockProductionLot.count("name like ?", new String[]{"1%"}) % 10000);
            int newLotId = stockProductionLot.insert(lotValues);
            ODataRow record = records.get(0);
                if (record.getFloat("qty").equals(record.getFloat("input_qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", inputValues.getInt("pretreatment_location_id"));
                    stockQuant.update(record.getInt("_id"), values);
                } else { // Part processing
                    // Remain
                    OValues remainValues = new OValues();
                    remainValues.put("lot_id", record.getInt("lot_id"));
                    remainValues.put("location_id", record.getInt("location_id"));
                    remainValues.put("qty", record.getFloat("qty") - record.getFloat("input_qty"));
                    stockQuant.update(record.getInt("_id"), remainValues);
                    OValues newValues = new OValues();
                    newValues.put("lot_id", record.getInt("lot_id"));
                    newValues.put("location_id", inputValues.getInt("pretreatment_location_id"));
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }
                // New Quants with new lot
                OValues newQuantValues = new OValues();
                newQuantValues.put("lot_id", newLotId);
                newQuantValues.put("location_id", inputValues.getInt("destination_location_id"));
                newQuantValues.put("qty", record.getFloat("input_qty"));
                int newQuantId = stockQuant.insert(newQuantValues);

                // Create the wizard record
//                wizardValues.put("quant_line_quantity", RecordUtils.getFieldString(records, "input_qty"));
                wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
            wizardValues.put("before_ids", RecordUtils.getOriginIds(records));
            wizardValues.put("new_quant_ids", String.valueOf(newQuantId));
                wizardValues.put("pretreatment_location_id", inputValues.getInt("pretreatment_location_id"));
                wizardValues.put("destination_location_id", inputValues.getInt("destination_location_id"));
                wizardValues.put("qty", quantity);
                wizardValues.put("remain_qty", remainQuantity);
                wizardValues.put("new_prodlot_ids", String.valueOf(newLotId));

               super.performProcessing();
        }
    }
}
