/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chen.deskclock.actionbarmenu;

import android.view.Menu;
import android.view.MenuItem;

import com.chen.deskclock.DeskClock;
import com.chen.deskclock.R;
import com.chen.deskclock.uidata.UiDataModel;

import static android.view.Menu.NONE;

/**
 * {@link MenuItemController} that surfaces the primary action of the selected tab (the same action
 * as the floating action button) in the overflow menu: "New alarm", "Add city" or "New timer".
 */
public final class FabActionMenuItemController implements MenuItemController {

    private static final int FAB_ACTION_MENU_RES_ID = R.id.menu_item_fab_action;

    private final DeskClock mDeskClock;

    public FabActionMenuItemController(DeskClock deskClock) {
        mDeskClock = deskClock;
    }

    @Override
    public int getId() {
        return FAB_ACTION_MENU_RES_ID;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
        menu.add(NONE, FAB_ACTION_MENU_RES_ID, NONE, R.string.menu_item_new_alarm)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public void onPrepareOptionsItem(MenuItem item) {
        final int titleResId;
        boolean visible = true;
        switch (UiDataModel.getUiDataModel().getSelectedTab()) {
            case ALARMS:
                titleResId = R.string.menu_item_new_alarm;
                break;
            case CLOCKS:
                titleResId = R.string.menu_item_add_city;
                break;
            case TIMERS:
                titleResId = R.string.menu_item_new_timer;
                break;
            default:
                // The stopwatch tab has no discrete "create" action.
                titleResId = R.string.menu_item_new_alarm;
                visible = false;
                break;
        }
        item.setTitle(titleResId);
        item.setVisible(visible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != FAB_ACTION_MENU_RES_ID) {
            return false;
        }
        mDeskClock.performSelectedFabAction();
        return true;
    }
}
