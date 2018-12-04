package com.theNewCone.cricketScoreCard.fragment;


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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.theNewCone.cricketScoreCard.R;
import com.theNewCone.cricketScoreCard.activity.PlayerSelectActivity;
import com.theNewCone.cricketScoreCard.activity.TeamSelectActivity;
import com.theNewCone.cricketScoreCard.intf.ConfirmationDialogClickListener;
import com.theNewCone.cricketScoreCard.intf.DialogItemClickListener;
import com.theNewCone.cricketScoreCard.intf.DrawerController;
import com.theNewCone.cricketScoreCard.match.Team;
import com.theNewCone.cricketScoreCard.player.Player;
import com.theNewCone.cricketScoreCard.utils.CommonUtils;
import com.theNewCone.cricketScoreCard.utils.database.DatabaseHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayerFragment extends Fragment
		implements DialogItemClickListener, View.OnClickListener, ConfirmationDialogClickListener {
    TextView tvBatStyle, tvBowlStyle, tvTeams;
    EditText etName, etAge;
    CheckBox cbIsWK;
    Player selPlayer;

    Button btnDelete;
    List<Integer> associatedToTeams;
    Team[] selTeams;

    private static final int REQ_CODE_TEAM_SELECT = 1;
	private static final int REQ_CODE_DISPLAY_ALL_PLAYERS = 2;

	private static final int CONFIRMATION_DELETE_PLAYER = 1;

    public PlayerFragment() {
        setHasOptionsMenu(true);
        // Required empty public constructor
    }

    public static PlayerFragment newInstance() {
        return new PlayerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_player, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_getPlayerList:
            	Intent intent = new Intent(getContext(), PlayerSelectActivity.class);
            	intent.putExtra(PlayerSelectActivity.ARG_PLAYER_LIST, new DatabaseHandler(getContext()).getAllPlayers().toArray());
            	startActivityForResult(intent, REQ_CODE_DISPLAY_ALL_PLAYERS);
                break;

			case R.id.menu_updateTeams:
				showTeamsSelectDialog();
				break;

		}

        return true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View theView = inflater.inflate(R.layout.fragment_player, container, false);

		loadViews(theView);

		populateData();

		if(getActivity() != null) {
			DrawerController drawerController = (DrawerController) getActivity();
			drawerController.setDrawerEnabled(true);
			drawerController.enableAllDrawerMenuItems();
			drawerController.disableDrawerMenuItem(R.id.nav_manage_player);
			getActivity().setTitle(getString(R.string.title_fragment_manage_player));
		}

        return theView;
    }

    @Override
    public void onItemSelect(String type, String value, int position) {
        switch (type) {
            case StringDialog.ARG_TYPE_BAT_STYLE:
                tvBatStyle.setText(value);
                break;

            case StringDialog.ARG_TYPE_BOWL_STYLE:
                tvBowlStyle.setText(value);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvPlayerBatStyle:
                showBattingStyleDialog();
                break;

            case R.id.tvPlayerBowlStyle:
                showBowlingStyleDialog();
                break;

            case R.id.btnPlayerSave:
                savePlayer();
                break;

            case R.id.btnPlayerClear:
                clearScreen();
                break;

            case R.id.btnPlayerDelete:
                confirmDeletePlayer();
                break;
        }
    }

	@Override
	public void onDestroyView() {
		selPlayer = null;
		super.onDestroyView();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case REQ_CODE_TEAM_SELECT:
				if(resultCode == TeamSelectActivity.RESP_CODE_OK) {
					Team[] teams = CommonUtils.objectArrToTeamArr((Object []) data.getSerializableExtra(TeamSelectActivity.ARG_RESP_TEAMS));

					if(teams !=  null) {
						List<Integer> teamIDList = new ArrayList<>();
						selTeams = teams;

						for (Team team : teams)
							teamIDList.add(team.getId());

						selPlayer.setTeamsAssociatedTo(teamIDList);

						populateData();
					}
				}
				break;

			case REQ_CODE_DISPLAY_ALL_PLAYERS:
				if(resultCode == PlayerSelectActivity.RESP_CODE_OK) {
					selPlayer = (Player) data.getSerializableExtra(PlayerSelectActivity.ARG_RESP_SEL_PLAYER);
					selPlayer = new DatabaseHandler(getContext()).getPlayer(selPlayer.getID());
					populateData();
				}
				break;
		}
	}

	@Override
	public void onConfirmationClick(int confirmationCode, boolean accepted) {
		switch (confirmationCode) {
			case CONFIRMATION_DELETE_PLAYER:
				if(accepted)
					deletePlayer();
		}
	}

	private void loadViews(View theView) {
		tvBatStyle = theView.findViewById(R.id.tvPlayerBatStyle);
		tvBowlStyle = theView.findViewById(R.id.tvPlayerBowlStyle);
		tvTeams = theView.findViewById(R.id.tvTeams);

		etName = theView.findViewById(R.id.etPlayerName);
		etAge = theView.findViewById(R.id.etPlayerAge);

		cbIsWK = theView.findViewById(R.id.cbIsPlayerWK);

		btnDelete = theView.findViewById(R.id.btnPlayerDelete);

		tvBatStyle.setOnClickListener(this);
		tvBowlStyle.setOnClickListener(this);

		theView.findViewById(R.id.btnPlayerSave).setOnClickListener(this);
		theView.findViewById(R.id.btnPlayerClear).setOnClickListener(this);
		btnDelete.setOnClickListener(this);
	}

	private void showBattingStyleDialog() {
		if(getFragmentManager() != null) {
			Player.BattingType[] battingTypes = Player.BattingType.values();
			String[] battingStyles = new String[battingTypes.length-1];

			int i=0;
			for(Player.BattingType type : battingTypes)
				if(type != Player.BattingType.NOT_SELECTED)
					battingStyles[i++] = type.toString();

			StringDialog dialog = StringDialog.newInstance("Select Batting Style", battingStyles, StringDialog.ARG_TYPE_BAT_STYLE);
			dialog.setDialogItemClickListener(this);
			dialog.show(getFragmentManager(), StringDialog.ARG_TYPE_BAT_STYLE + "Dialog");
		}
	}

	private void showBowlingStyleDialog() {
		if(getFragmentManager() != null) {
			Player.BowlingType[] bowlingTypes = Player.BowlingType.values();
			String[] bowlingStyles = new String[bowlingTypes.length-1];

			int i=0;
			for(Player.BowlingType type : bowlingTypes)
				if(type != Player.BowlingType.NOT_SELECTED && type != Player.BowlingType.NONE)
					bowlingStyles[i++] = type.toString();

			bowlingStyles[i] = Player.BowlingType.NONE.toString();

			StringDialog dialog = StringDialog.newInstance("Select Bowling Style", bowlingStyles, StringDialog.ARG_TYPE_BOWL_STYLE);
			dialog.setDialogItemClickListener(this);
			dialog.show(getFragmentManager(), StringDialog.ARG_TYPE_BOWL_STYLE + "Dialog");
		}
	}

	private void populateData() {
		if(selPlayer != null) {
			etName.setText(selPlayer.getName());
			etAge.setText(selPlayer.getAge() > 0 ? String.valueOf(selPlayer.getAge()) : "");
			tvBatStyle.setText(selPlayer.getBattingStyle() == Player.BattingType.NOT_SELECTED
					? getString(R.string.selectBatStyle)
					: selPlayer.getBattingStyle().toString());
			tvBowlStyle.setText(selPlayer.getBowlingStyle() == Player.BowlingType.NOT_SELECTED
					? getString(R.string.selectBowlStyle)
					: selPlayer.getBowlingStyle().toString());
			cbIsWK.setChecked(selPlayer.isWicketKeeper());

			int numPlayers = (selPlayer.getTeamsAssociatedTo() != null)
					? selPlayer.getTeamsAssociatedTo().size()
					: (associatedToTeams != null ? associatedToTeams.size() : -1);

			tvTeams.setText(numPlayers > -1 ? String.valueOf(numPlayers) : "None");

			if (selPlayer.getID() >= 0) {
				btnDelete.setVisibility(View.VISIBLE);
			}
		} else {
			etName.setText("");
			etAge.setText("");
			tvBatStyle.setText(R.string.selectBatStyle);
			tvBowlStyle.setText(R.string.selectBowlStyle);
			cbIsWK.setChecked(false);
			btnDelete.setVisibility(View.INVISIBLE);
			tvTeams.setText(getString(R.string.none));

			etName.requestFocus();
		}
	}

	private void showTeamsSelectDialog() {
		storePlayer();

		Intent batsmanIntent = new Intent(getContext(), TeamSelectActivity.class);

		batsmanIntent.putExtra(TeamSelectActivity.ARG_IS_MULTI, true);
		associatedToTeams = selPlayer.getTeamsAssociatedTo();
		if(associatedToTeams !=  null) {
			batsmanIntent.putIntegerArrayListExtra(TeamSelectActivity.ARG_EXISTING_TEAMS, (ArrayList<Integer>) associatedToTeams);
		}

		startActivityForResult(batsmanIntent, REQ_CODE_TEAM_SELECT);
    }

    private void storePlayer() {
    	int playerID = selPlayer != null ? selPlayer.getID() : -1;
    	int age = (etAge.getText().toString().trim().length() > 0) ? Integer.parseInt(etAge.getText().toString()) : -1;
    	Player.BattingType batStyle = tvBatStyle.getText().toString().equals(getString(R.string.selectBatStyle))
				? Player.BattingType.NOT_SELECTED
				: Player.BattingType.valueOf(tvBatStyle.getText().toString());
    	Player.BowlingType bowlStyle = tvBowlStyle.getText().toString().equals(getString(R.string.selectBowlStyle))
				? Player.BowlingType.NOT_SELECTED
				: Player.BowlingType.valueOf(tvBowlStyle.getText().toString());

    	List<Integer> teamIDList = selPlayer != null ? selPlayer.getTeamsAssociatedTo() : null;

    	selPlayer = new Player(playerID, etName.getText().toString(), age, batStyle, bowlStyle, cbIsWK.isChecked());
    	selPlayer.setTeamsAssociatedTo(teamIDList);
    }

	private void savePlayer() {
		storePlayer();

        DatabaseHandler dbHandler = new DatabaseHandler(getContext());

        StringBuilder errorSB = new StringBuilder();

        if(etName.getText().toString().trim().length() < 3)
            errorSB.append("Enter valid name (at-least 3 characters)\n");
        /*if(etAge.getText().toString().trim().length() < 1)
            errorSB.append("Enter valid age\n");*/
        if(tvBatStyle.getText().toString().equals(getString(R.string.selectBatStyle)))
            errorSB.append("Select Batting Style\n");
        if(tvBowlStyle.getText().toString().equals(getString(R.string.selectBowlStyle)))
            errorSB.append("Select Bowling Style\n");

        if(errorSB.toString().trim().length() == 0) {
        	boolean isNew = selPlayer.getID() < 0;
            int playerID = dbHandler.upsertPlayer(selPlayer);

            if (playerID == dbHandler.CODE_INS_PLAYER_DUP_RECORD) {
                Toast.makeText(getContext(),
						"Player with same name exists.\nChange name or update/delete existing team.",
						Toast.LENGTH_LONG).show();
            } else {
				selPlayer.setPlayerID(playerID);
				dbHandler.updateTeamList(selPlayer,
						getAddedTeams(selTeams, associatedToTeams), getRemovedTeams(selTeams, associatedToTeams));

                Toast.makeText(getContext(), "Player Saved Successfully", Toast.LENGTH_SHORT).show();
                selPlayer = isNew ? null : selPlayer;
                populateData();
            }
        } else {
            Toast.makeText(getContext(), errorSB.toString().trim(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearScreen() {
        selPlayer = null;
        populateData();
    }

    private void deletePlayer() {
        DatabaseHandler dbHandler = new DatabaseHandler(getContext());
        boolean success = dbHandler.deletePlayer(selPlayer.getID());

        if(success) {
            Toast.makeText(getContext(), "Record Deleted Successfully", Toast.LENGTH_SHORT).show();
            clearScreen();
        } else {
            Toast.makeText(getContext(), "Problem deleting Data", Toast.LENGTH_SHORT).show();
        }
    }

	private List<Integer> getAddedTeams(Team[] selTeams, List<Integer> pastTeams) {
		List<Integer> newTeams = new ArrayList<>();
		if(pastTeams == null)
			pastTeams = new ArrayList<>();

		if(selTeams != null) {
			for (Team team : selTeams) {
				if (!pastTeams.contains(team.getId())) {
					newTeams.add(team.getId());
				}
			}
		}

		return newTeams;
	}

	private List<Integer> getRemovedTeams(Team[] selTeams, List<Integer> pastTeams) {
		List<Integer> removedTeams = new ArrayList<>();
		if(pastTeams == null)
			pastTeams = new ArrayList<>();

		List<Integer> selPlayerIDs = new ArrayList<>();
		if(selTeams != null) {
			for (Team teamT : selTeams)
				selPlayerIDs.add(teamT.getId());
		}

		for(int pastPlayer : pastTeams) {
			if(!selPlayerIDs.contains(pastPlayer))
				removedTeams.add(pastPlayer);
		}

		return removedTeams;
	}

	private void confirmDeletePlayer() {
    	if(getFragmentManager() != null) {
			ConfirmationDialog dialog = ConfirmationDialog.newInstance(CONFIRMATION_DELETE_PLAYER, "Confirm Delete", "Are you sure you want to delete the player?");
			dialog.setConfirmationClickListener(this);
			dialog.show(getFragmentManager(), "ConfirmPlayerDeleteDialog");
		}
	}
}