package com.thenewcone.myscorecard.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.thenewcone.myscorecard.R;
import com.thenewcone.myscorecard.intf.DialogItemClickListener;
import com.thenewcone.myscorecard.utils.database.DatabaseHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener, DialogItemClickListener {

	private static final String STRING_DIALOG_LOAD_SAVED_MATCHES = "LoadSavedMatches";
	DatabaseHandler dbHandler;
	SparseArray<String> savedMatchDataList;

	public static HomeFragment newInstance() {
		return new HomeFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
        View theView;
		theView = inflater.inflate(R.layout.home_fragment, container, false);

		theView.findViewById(R.id.btnNewMatch).setOnClickListener(this);
		theView.findViewById(R.id.btnManagePlayer).setOnClickListener(this);
        theView.findViewById(R.id.btnManageTeam).setOnClickListener(this);
        theView.findViewById(R.id.btnLoadMatch).setOnClickListener(this);

		dbHandler = new DatabaseHandler(getContext());

		return theView;
	}

	@Override
	public void onClick(View view) {
		if(getActivity() != null) {
			FragmentManager fragMgr = getActivity().getSupportFragmentManager();
            String fragmentTag;

			switch(view.getId()) {

                case R.id.btnNewMatch:
                    fragmentTag = NewMatchFragment.class.getSimpleName();
                    fragMgr.beginTransaction()
                            .replace(R.id.frame_container, NewMatchFragment.newInstance(), fragmentTag)
                            .addToBackStack(fragmentTag)
                            .commit();
                    break;

                case R.id.btnManageTeam:
                    fragmentTag = TeamFragment.class.getSimpleName();
                    fragMgr.beginTransaction()
                            .replace(R.id.frame_container, TeamFragment.newInstance(), fragmentTag)
                            .addToBackStack(fragmentTag)
                            .commit();
                    break;

                case R.id.btnManagePlayer:
                    fragmentTag = PlayerFragment.class.getSimpleName();
                    fragMgr.beginTransaction()
                            .replace(R.id.frame_container, PlayerFragment.newInstance(), fragmentTag)
                            .addToBackStack(fragmentTag)
                            .commit();
                    break;

				case R.id.btnLoadMatch:
					showSavedMatchDialog();

			}
		}
	}

	private void showSavedMatchDialog() {
		if(getFragmentManager() != null) {
			savedMatchDataList = dbHandler.getSavedMatches(DatabaseHandler.SAVE_MANUAL, 0, null);
			if (savedMatchDataList != null && savedMatchDataList.size() > 0) {
				List<String> savedMatches = new ArrayList<>();
				for (int i = 0; i < savedMatchDataList.size(); i++)
					savedMatches.add(savedMatchDataList.valueAt(i));

				Collections.sort(savedMatches);

				StringDialog dialog = StringDialog.newInstance("Select Match to Load", (String[]) savedMatches.toArray(), STRING_DIALOG_LOAD_SAVED_MATCHES);
				dialog.setDialogItemClickListener(this);
				dialog.show(getFragmentManager(), "SavedMatchDialog");
			} else {
				Toast.makeText(getContext(), "No Saved Matches Found", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onItemSelect(String type, String value, int position) {
		switch (type) {
			case STRING_DIALOG_LOAD_SAVED_MATCHES:
				if(getActivity() != null) {
					int matchStateID = savedMatchDataList.indexOfValue(value);
					String fragmentTag = NewMatchFragment.class.getSimpleName();
					getActivity().getSupportFragmentManager().beginTransaction()
							.replace(R.id.frame_container, LimitedOversFragment.loadInstance(matchStateID), fragmentTag)
							.addToBackStack(fragmentTag)
							.commit();
				}
				break;
		}
	}
}
