
package com.thenewcone.myscorecard.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.thenewcone.myscorecard.R;
import com.thenewcone.myscorecard.activity.PlayerSelectActivity;
import com.thenewcone.myscorecard.activity.TeamSelectActivity;
import com.thenewcone.myscorecard.intf.ConfirmationDialogClickListener;
import com.thenewcone.myscorecard.intf.DialogItemClickListener;
import com.thenewcone.myscorecard.intf.DrawerLocker;
import com.thenewcone.myscorecard.match.Team;
import com.thenewcone.myscorecard.player.Player;
import com.thenewcone.myscorecard.utils.CommonUtils;
import com.thenewcone.myscorecard.utils.database.AddDBData;
import com.thenewcone.myscorecard.utils.database.DatabaseHandler;

import java.util.ArrayList;
import java.util.List;

public class TeamFragment extends Fragment
    implements View.OnClickListener, DialogItemClickListener, ConfirmationDialogClickListener {

	private final int REQ_CODE_TEAM_SELECT = 1;
	private final int REQ_CODE_UPDATE_PLAYERS = 2;

	private static final int CONFIRMATION_DELETE_TEAM = 1;

    Button btnSaveTeam, btnDeleteTeam, btnReset;
    EditText etTeamName, etShortName;
    TextView tvPlayers;

    List<Team> teamList;
    Team selTeam;
    List<Integer> associatedPlayers = new ArrayList<>();
    Player[] selPlayers;

    public TeamFragment() {
		setHasOptionsMenu(true);
    }

    public static TeamFragment newInstance() {
        return new TeamFragment();
    }

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_fragment_team, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_getTeamList:
				showTeamsSelectDialog();
				break;

			case R.id.menu_updatePlayers:
				if(selTeam != null && selTeam.getId() > 0)
					showPlayerListDialog();
				else
					Toast.makeText(getContext(), "Select/Create a team to update player list", Toast.LENGTH_SHORT).show();
				break;

			case R.id.menu_loadData:
				AddDBData addData = new AddDBData(getContext());
				if(addData.addTeams())
					Toast.makeText(getContext(), "Data uploaded successfully", Toast.LENGTH_SHORT).show();
				break;
		}

		return true;
	}


	@Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
        View theView = inflater.inflate(R.layout.content_team, container, false);

        etTeamName = theView.findViewById(R.id.etTeamName);
        etShortName = theView.findViewById(R.id.etTeamShortName);

		tvPlayers = theView.findViewById(R.id.tvPlayers);

        btnSaveTeam = theView.findViewById(R.id.btnSaveTeam);
        btnDeleteTeam = theView.findViewById(R.id.btnDeleteTeam);
        btnReset = theView.findViewById(R.id.btnResetData);

        btnSaveTeam.setOnClickListener(this);
        btnDeleteTeam.setOnClickListener(this);
        btnReset.setOnClickListener(this);

		if(getActivity() != null)
			((DrawerLocker) getActivity()).setDrawerEnabled(true);

        return theView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSaveTeam:
            	saveTeam();
                break;

            case R.id.btnDeleteTeam:
            	confirmDeleteTeam();
                break;

			case R.id.btnResetData:
				selTeam = null;
				populateData();
				break;
        }
    }

	private void populateData() {
    	if(selTeam != null) {
			etTeamName.setText(selTeam.getName());
			etShortName.setText(selTeam.getShortName());
			tvPlayers.setText(String.valueOf(associatedPlayers.size()));

			btnDeleteTeam.setVisibility(View.VISIBLE);
		} else {
			etTeamName.setText("");
			etShortName.setText("");
			tvPlayers.setText(getString(R.string.none));
			etTeamName.requestFocus();

			btnDeleteTeam.setVisibility(View.GONE);
		}
	}

    @Override
    public void onItemSelect(String type, String value, int position) {
        selTeam = teamList.get(position);
    }

    private void saveTeam() {
		String teamName = etTeamName.getText().toString();
		String shortName = etShortName.getText().toString();

		String errorMessage = null;
		if(teamName.length() < 5) {
			errorMessage = "Enter valid team name (at-least 5 characters)";
		} else if(shortName.length() < 2) {
			errorMessage = "Enter valid team name (at-least 2 characters)";
		}

		if(errorMessage != null) {
			Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
		} else {
			int teamID = selTeam != null ? selTeam.getId() : -1;
			selTeam = new Team(teamName, shortName);
			if (teamID > -1)
				selTeam.setId(teamID);

			boolean isNew = teamID < 0;
			DatabaseHandler dbHandler = new DatabaseHandler(getContext());
			int rowID = dbHandler.upsertTeam(selTeam);

			if (rowID == dbHandler.CODE_NEW_TEAM_DUP_RECORD) {
				Toast.makeText(getContext(), "Team with same name already exists. Choose a different name.", Toast.LENGTH_SHORT).show();
			} else {

				if (selPlayers != null && selPlayers.length > 0) {
					List<Integer> addedPlayers = getAddedPlayers(selPlayers, associatedPlayers);
					List<Integer> removedPlayers = getRemovedPlayers(selPlayers, associatedPlayers);

					dbHandler.updateTeamList(selTeam, addedPlayers, removedPlayers);
					associatedPlayers.clear();
					for (Player player : dbHandler.getTeamPlayers(selTeam.getId()))
						associatedPlayers.add(player.getID());
				}
				Toast.makeText(getContext(), "Team saved successfully.", Toast.LENGTH_SHORT).show();
				selTeam = isNew ? null : selTeam;
				populateData();
			}
		}
	}

	private void deleteTeam() {
		DatabaseHandler dbHandler = new DatabaseHandler(getContext());
    	boolean success = dbHandler.deleteTeam(selTeam.getId());

    	if(success) {
			Toast.makeText(getContext(), "Team deleted successfully", Toast.LENGTH_SHORT).show();
			selTeam = null;
			populateData();
		} else {
			Toast.makeText(getContext(), "Team deletion failed", Toast.LENGTH_SHORT).show();
		}
	}

	private void showTeamsSelectDialog() {
		Intent batsmanIntent = new Intent(getContext(), TeamSelectActivity.class);

		batsmanIntent.putExtra(TeamSelectActivity.ARG_IS_MULTI, false);

		startActivityForResult(batsmanIntent, REQ_CODE_TEAM_SELECT);
	}

	private void showPlayerListDialog() {
		DatabaseHandler dbHandler = new DatabaseHandler(getContext());

		List<Integer> currAssociation = new ArrayList<>();
		if(selPlayers != null && selPlayers.length > 0) {
			for (Player player : selPlayers)
				currAssociation.add(player.getID());
		} else{
			currAssociation = associatedPlayers;
		}


    	Intent updPlayersIntent = new Intent(getContext(), PlayerSelectActivity.class);
    	updPlayersIntent.putExtra(PlayerSelectActivity.ARG_PLAYER_LIST, dbHandler.getAllPlayers().toArray());
    	updPlayersIntent.putExtra(PlayerSelectActivity.ARG_IS_MULTI_SELECT, true);
    	updPlayersIntent.putIntegerArrayListExtra(PlayerSelectActivity.ARG_ASSOCIATED_PLAYERS,
				(ArrayList<Integer>) currAssociation);

    	startActivityForResult(updPlayersIntent, REQ_CODE_UPDATE_PLAYERS);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		DatabaseHandler dbHandler = new DatabaseHandler(getContext());
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case REQ_CODE_TEAM_SELECT:
				if(resultCode == TeamSelectActivity.RESP_CODE_OK) {
					selTeam = (Team) data.getSerializableExtra(TeamSelectActivity.ARG_RESP_TEAM);
					selPlayers = null;

					associatedPlayers = dbHandler.getAssociatedPlayers(selTeam.getId());

					populateData();
				}
				break;

			case REQ_CODE_UPDATE_PLAYERS:
				if(resultCode == PlayerSelectActivity.RESP_CODE_OK) {
					selPlayers =
							CommonUtils.objectArrToPlayerArr((Object[]) data.getSerializableExtra(PlayerSelectActivity.ARG_RESP_SEL_PLAYERS));

					tvPlayers.setText(String.valueOf(selPlayers != null && selPlayers.length > 0 ? selPlayers.length : getString(R.string.none)));
				}
		}
	}

	private List<Integer> getAddedPlayers(Player[] selPlayers, List<Integer> pastPlayers) {
    	List<Integer> newPlayers = new ArrayList<>();
		if(pastPlayers == null)
			pastPlayers = new ArrayList<>();

    	if(selPlayers != null) {
			for (Player player : selPlayers) {
				if (!pastPlayers.contains(player.getID())) {
					newPlayers.add(player.getID());
				}
			}
		}

		return newPlayers;
	}

	private List<Integer> getRemovedPlayers(Player[] selPlayers, List<Integer> pastPlayers) {
    	List<Integer> removedPlayers = new ArrayList<>();
		if(pastPlayers == null)
			pastPlayers = new ArrayList<>();

    	List<Integer> selPlayerIDs = new ArrayList<>();
    	if(selPlayers != null) {
			for (Player player : selPlayers)
				selPlayerIDs.add(player.getID());
		}

		for(int pastPlayer : pastPlayers) {
    		if(!selPlayerIDs.contains(pastPlayer))
    			removedPlayers.add(pastPlayer);
		}

		return removedPlayers;
	}

	private void confirmDeleteTeam() {
		if(getFragmentManager() != null) {
			ConfirmationDialog dialog = ConfirmationDialog.newInstance(CONFIRMATION_DELETE_TEAM, "Confirm Delete", "Are you sure you want to delete the player?");
			dialog.setConfirmationClickListener(this);
			dialog.show(getFragmentManager(), "ConfirmTeamDeleteDialog");
		}
	}

	@Override
	public void onConfirmationClick(int confirmationCode, boolean accepted) {
		switch (confirmationCode) {
			case CONFIRMATION_DELETE_TEAM:
				if(accepted)
					deleteTeam();
		}
	}
}
