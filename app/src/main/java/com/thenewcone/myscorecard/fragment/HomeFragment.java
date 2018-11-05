package com.thenewcone.myscorecard.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.thenewcone.myscorecard.R;
import com.thenewcone.myscorecard.activity.SavedMatchSelectActivity;
import com.thenewcone.myscorecard.intf.ConfirmationDialogClickListener;
import com.thenewcone.myscorecard.intf.DrawerLocker;
import com.thenewcone.myscorecard.match.MatchState;
import com.thenewcone.myscorecard.utils.CommonUtils;
import com.thenewcone.myscorecard.utils.database.AddDBData;
import com.thenewcone.myscorecard.utils.database.DatabaseHandler;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements View.OnClickListener, ConfirmationDialogClickListener {

	//private static final String STRING_DIALOG_LOAD_SAVED_MATCHES = "LoadSavedMatches";

	private static final int REQ_CODE_MATCH_LIST_LOAD = 1;
	private static final int REQ_CODE_MATCH_LIST_DELETE = 2;

	private static final int CONFIRMATION_CODE_DELETE_MATCHES = 1;

	DatabaseHandler dbHandler;

	MatchState[] matchesToDelete;

	public static HomeFragment newInstance() {
		return new HomeFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
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
        theView.findViewById(R.id.btnDeleteMatches).setOnClickListener(this);

		dbHandler = new DatabaseHandler(getContext());

		if(getActivity() != null)
			((DrawerLocker) getActivity()).setDrawerEnabled(true);

		return theView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_fragments_home, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_loadData:
				if(new AddDBData(getContext()).addAll())
					Toast.makeText(getContext(), "Data uploaded successfully", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getContext(), "Data upload failed", Toast.LENGTH_SHORT).show();
				break;

			case R.id.menu_quit:
				if(getActivity() != null) {
					getActivity().finishAndRemoveTask();
				}
				break;
		}
		return true;
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
					showSavedMatchDialog(false, REQ_CODE_MATCH_LIST_LOAD);
					break;

				case R.id.btnDeleteMatches:
					showSavedMatchDialog(true, REQ_CODE_MATCH_LIST_DELETE);
					break;
			}
		}
	}

	private void showSavedMatchDialog(boolean isMulti, int requestCode) {
		List<MatchState> savedMatchDataList = dbHandler.getSavedMatches(DatabaseHandler.SAVE_MANUAL, 0, null);
		if(savedMatchDataList != null && savedMatchDataList.size() > 0) {
			Intent getMatchListIntent = new Intent(getContext(), SavedMatchSelectActivity.class);
			getMatchListIntent.putExtra(SavedMatchSelectActivity.ARG_MATCH_LIST, savedMatchDataList.toArray());
			getMatchListIntent.putExtra(SavedMatchSelectActivity.ARG_IS_MULTI_SELECT, isMulti);
			startActivityForResult(getMatchListIntent, requestCode);
		} else {
			Toast.makeText(getContext(), "No Saved matches found.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case REQ_CODE_MATCH_LIST_LOAD:
				if(resultCode == SavedMatchSelectActivity.RESP_CODE_OK) {
					MatchState selSavedMatch = (MatchState) data.getSerializableExtra(SavedMatchSelectActivity.ARG_RESP_SEL_MATCH);
					if(getActivity() != null) {
						String fragmentTag = NewMatchFragment.class.getSimpleName();

						getActivity().getSupportFragmentManager().beginTransaction()
								.replace(R.id.frame_container, LimitedOversFragment.loadInstance(selSavedMatch.getId()), fragmentTag)
								.addToBackStack(fragmentTag)
								.commit();
					}
				}
				break;

			case REQ_CODE_MATCH_LIST_DELETE:
				if(resultCode == SavedMatchSelectActivity.RESP_CODE_OK) {
					matchesToDelete = CommonUtils.objectArrToMatchStateArr(
							(Object[]) data.getSerializableExtra(SavedMatchSelectActivity.ARG_RESP_SEL_MATCHES));

					if(matchesToDelete.length > 0 && getFragmentManager() != null) {
						ConfirmationDialog confirmationDialog = ConfirmationDialog.newInstance(CONFIRMATION_CODE_DELETE_MATCHES,
								"Confirm Delete", String.format(Locale.getDefault(), "Do you want to delete these %d saved matches?", matchesToDelete.length));
						confirmationDialog.setConfirmationClickListener(this);
						confirmationDialog.show(getFragmentManager(), "DeleteSavedMatches");
					}
				}
		}
	}

	@Override
	public void onConfirmationClick(int confirmationCode, boolean accepted) {
		switch (confirmationCode) {
			case CONFIRMATION_CODE_DELETE_MATCHES:
				if(accepted) {
					if(dbHandler.deleteSavedMatchStates(matchesToDelete)) {
						Toast.makeText(getContext(), "Matches Deleted", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getContext(), "Unable to delete all matches. Please retry", Toast.LENGTH_LONG).show();
					}
				}
				break;
		}
	}
}
