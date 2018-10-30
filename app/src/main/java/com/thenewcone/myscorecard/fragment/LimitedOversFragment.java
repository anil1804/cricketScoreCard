package com.thenewcone.myscorecard.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.thenewcone.myscorecard.activity.BatsmanSelectActivity;
import com.thenewcone.myscorecard.activity.BowlerSelectActivity;
import com.thenewcone.myscorecard.activity.ExtraDialogActivity;
import com.thenewcone.myscorecard.R;
import com.thenewcone.myscorecard.activity.InputActivity;
import com.thenewcone.myscorecard.activity.WicketDialogActivity;
import com.thenewcone.myscorecard.intf.ConfirmationDialogClickListener;
import com.thenewcone.myscorecard.intf.DialogItemClickListener;
import com.thenewcone.myscorecard.match.CricketCard;
import com.thenewcone.myscorecard.match.CricketCardUtils;
import com.thenewcone.myscorecard.match.Team;
import com.thenewcone.myscorecard.player.BatsmanStats;
import com.thenewcone.myscorecard.player.BowlerStats;
import com.thenewcone.myscorecard.player.Player;
import com.thenewcone.myscorecard.scorecard.Extra;
import com.thenewcone.myscorecard.scorecard.WicketData;
import com.thenewcone.myscorecard.utils.CommonUtils;
import com.thenewcone.myscorecard.utils.database.DatabaseHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LimitedOversFragment extends Fragment
	implements View.OnClickListener, ConfirmationDialogClickListener, DialogItemClickListener {
	View theView;
	WicketData.DismissalType dismissalType;
    DatabaseHandler dbHandler;

    private static final int REQ_CODE_EXTRA_DIALOG = 1;
	private static final int REQ_CODE_WICKET_DIALOG = 2;
    private static final int REQ_CODE_BATSMAN_DIALOG = 3;
    private static final int REQ_CODE_BOWLER_DIALOG = 4;
    private static final int REQ_CODE_CURRENT_FACING_DIALOG = 5;
    private static final int REQ_CODE_GET_SAVE_MATCH_NAME = 6;

    private static final int CONFIRMATION_CODE_SAVE_MATCH = 1;

    private static final String STRING_DIALOG_LOAD_SAVED_MATCHES = "LoadSavedMatches";

	CricketCardUtils ccUtils;
    BatsmanStats newBatsman, outBatsman;

    TableRow trBatsman1, trBatsman2;
    TextView tvCurrScore, tvOvers, tvCRR, tvRRR;
    TextView tvBat1Name, tvBat1Runs, tvBat1Balls, tvBat14s, tvBat16s, tvBat1SR;
    TextView tvBat2Name, tvBat2Runs, tvBat2Balls, tvBat24s, tvBat26s, tvBat2SR;
    TextView tvLegByes, tvByes, tvWides, tvNoBalls, tvPenalty;
    TextView tvBowlName, tvBowlOvers, tvBowlMaidens, tvBowlRuns, tvBowlWickets, tvBowlEconomy;
    TextView tvResult, tvRunsInBalls;

    int maxWickets, matchID, currentUndoCount;
    boolean startInnings = true, isLoad = false;

	SparseArray<String> savedMatchDataList;

	public LimitedOversFragment() {
	}

	public static LimitedOversFragment loadInstance(int matchStateID) {
		LimitedOversFragment fragment = new LimitedOversFragment();
		fragment.loadSavedMatch(matchStateID);

		return fragment;
	}

	public static LimitedOversFragment newInstance(int matchID, String matchName, Team battingTeam, Team bowlingTeam, Team tossWonBy, int maxOvers, int maxWickets, int maxPerBowler) {

		LimitedOversFragment fragment = new LimitedOversFragment();

		fragment.initCricketCard(matchID, matchName, battingTeam, bowlingTeam, tossWonBy, maxOvers, maxWickets, maxPerBowler);

		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
        dbHandler = new DatabaseHandler(getContext());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_fragments_scorecard, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_save:
				if(!ccUtils.getCard().isInningsComplete()) {
					if(ccUtils.getCurrentFacing() == null || ccUtils.getOtherBatsman() == null) {
						Toast.makeText(getContext(), "Please Select Batsman before saving the match", Toast.LENGTH_LONG).show();
						break;
					}
					if(ccUtils.getBowler() == null) {
						Toast.makeText(getContext(), "Please Select Bowler before saving the match", Toast.LENGTH_LONG).show();
						break;
					}
				}
				showInputActivity();
				break;

			case R.id.menu_undo:
				if(currentUndoCount >= DatabaseHandler.maxUndoAllowed) {
					Toast.makeText(getContext(), "Maximum UNDO limit reached.", Toast.LENGTH_SHORT).show();
				} else if (currentUndoCount < CommonUtils.oversToBalls(Double.parseDouble(ccUtils.getCard().getTotalOversBowled()))) {
					Toast.makeText(getContext(), "No further undo possible.", Toast.LENGTH_SHORT).show();
				} else {
					currentUndoCount++;
					int matchStateID = dbHandler.getLastAutoSave(matchID);
					loadMatch(matchStateID);
					dbHandler.deleteMatch(matchStateID);
				}
				break;

			case R.id.menu_load:
				showSavedMatchDialog();
				break;
		}

		return true;
	}

	private void showInputActivity() {
		Intent iaIntent = new Intent(getContext(), InputActivity.class);
		startActivityForResult(iaIntent, REQ_CODE_GET_SAVE_MATCH_NAME);
	}

	private void loadMatch(int matchStateID) {
		String matchData = dbHandler.retrieveMatchData(matchStateID);
		if(matchData != null) {
			ccUtils = CommonUtils.convertToCCUtils(matchData);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		theView = inflater.inflate(R.layout.fragment_limited_overs, container, false);

		initialSetup();

		if(!isLoad)
			updateScreenForBatsmanSelect(View.GONE, View.VISIBLE, View.GONE);
		else {
			if(ccUtils.getCard().isInningsComplete())
				updateViewToCloseInnings();
		}

		return theView;
	}

	private void initialSetup() {
		updateCardDetails(true);

		theView.findViewById(R.id.btnRuns0).setOnClickListener(this);
		theView.findViewById(R.id.btnRuns1).setOnClickListener(this);
		theView.findViewById(R.id.btnRuns2).setOnClickListener(this);
		theView.findViewById(R.id.btnRuns3).setOnClickListener(this);
		theView.findViewById(R.id.btnRuns4).setOnClickListener(this);
		theView.findViewById(R.id.btnRuns6).setOnClickListener(this);
		theView.findViewById(R.id.btnWicket).setOnClickListener(this);
		theView.findViewById(R.id.btnExtrasLegByes).setOnClickListener(this);
		theView.findViewById(R.id.btnExtrasByes).setOnClickListener(this);
		theView.findViewById(R.id.btnExtrasWides).setOnClickListener(this);
		theView.findViewById(R.id.btnExtrasNoBall).setOnClickListener(this);
		theView.findViewById(R.id.btnExtraPenalty).setOnClickListener(this);
		theView.findViewById(R.id.btnSelBatsman).setOnClickListener(this);
        theView.findViewById(R.id.btnSelBowler).setOnClickListener(this);
        theView.findViewById(R.id.btnSelFacingBatsman).setOnClickListener(this);
        theView.findViewById(R.id.btnStartNextInnings).setOnClickListener(this);
	}

	public void loadSavedMatch(int matchStateID) {
		loadMatch(matchStateID);
		isLoad = true;
	}

	private void initCricketCard(int matchID, String matchName, Team battingTeam, Team bowlingTeam, Team tossWonBy, int maxOvers, int maxWickets, int maxPerBowler) {
		this.maxWickets = maxWickets;
		this.matchID = matchID;

		CricketCard card =
				new CricketCard(battingTeam.getShortName(), String.valueOf(maxOvers), maxPerBowler, maxWickets, 1);

		ccUtils = new CricketCardUtils(card, matchName, battingTeam, bowlingTeam);

		ccUtils.setTossWonBy(tossWonBy);
		ccUtils.setFirstInnings(battingTeam.getMatchPlayers(), bowlingTeam.getMatchPlayers());
	}

	private void updateCardDetails(boolean isInitial) {
		CricketCard currCard = ccUtils.getCard();

		/* Main Score Details*/
		if(isInitial) {
			TextView tvBattingTeam = theView.findViewById(R.id.tvBattingTeam);
			tvBattingTeam.setText(currCard.getBattingTeamName());

		}

		if(isInitial) {
			TableRow trTarget = theView.findViewById(R.id.trTarget);
			TableRow trRunsInBalls = theView.findViewById(R.id.trRunsInBalls);
			if(currCard.getInnings() == 1) {
				trTarget.setVisibility(View.GONE);
				trRunsInBalls.setVisibility(View.GONE);
			} else {
				trTarget.setVisibility(View.VISIBLE);
				trRunsInBalls.setVisibility(View.VISIBLE);
			}

            tvCurrScore = theView.findViewById(R.id.tvScore);
            tvOvers = theView.findViewById(R.id.tvOvers);
            tvCRR = theView.findViewById(R.id.tvCRR);
        }
		tvCurrScore.setText(String.valueOf(currCard.getScore() + "/" + currCard.getWicketsFallen()));
		tvOvers.setText(String.format(getString(R.string.tvOversText), currCard.getTotalOversBowled()));
		tvCRR.setText(CommonUtils.doubleToString(currCard.getRunRate(), "#.##"));

		/* Chasing Score Details*/
		tvRRR = theView.findViewById(R.id.tvRRR);
		if (isInitial) {
            TextView tvTarget = theView.findViewById(R.id.tvTarget);
            TextView tvMaxOvers = theView.findViewById(R.id.tvMaxOvers);
			if (currCard.getInnings() == 2) {
				tvTarget.setText(String.valueOf(currCard.getTarget()));
				tvMaxOvers.setText(String.format(getString(R.string.tvOversText), currCard.getMaxOvers()));
			} else {
				tvTarget.setText("-");
				tvRRR.setText("-");
				tvMaxOvers.setText("");
			}
		} else {
			if(currCard.getInnings() == 2) {
				tvRRR.setText(CommonUtils.doubleToString(currCard.getReqRate(), "#.##"));

			}
		}

		/* Batsman-1 Details*/
        if(isInitial) {
            trBatsman1 = theView.findViewById(R.id.trBatsman1);
            tvBat1Name = theView.findViewById(R.id.tvBat1Name);
            tvBat1Runs = theView.findViewById(R.id.tvBat1RunsScored);
            tvBat1Balls = theView.findViewById(R.id.tvBat1BallsFaced);
            tvBat14s = theView.findViewById(R.id.tvBat14sHit);
            tvBat16s = theView.findViewById(R.id.tvBat16sHit);
            tvBat1SR = theView.findViewById(R.id.tvBat1SR);
        }


		if(ccUtils.getCurrentFacing() != null) {
            trBatsman1.setVisibility(View.VISIBLE);
            tvBat1Name.setText(String.valueOf(ccUtils.getCurrentFacing().getBatsmanName() + " *"));
            tvBat1Runs.setText(String.valueOf(ccUtils.getCurrentFacing().getRunsScored()));
            tvBat1Balls.setText(String.valueOf(ccUtils.getCurrentFacing().getBallsPlayed()));
            tvBat14s.setText(String.valueOf(ccUtils.getCurrentFacing().getNum4s()));
            tvBat16s.setText(String.valueOf(ccUtils.getCurrentFacing().getNum6s()));
            tvBat1SR.setText(CommonUtils.doubleToString(ccUtils.getCurrentFacing().getStrikeRate(), "#.##"));
        } else {
            trBatsman1.setVisibility(View.GONE);
        }

		/* Batsman-2 Details*/
        if(isInitial) {
            trBatsman2 = theView.findViewById(R.id.trBatsman2);
            tvBat2Name = theView.findViewById(R.id.tvBat2Name);
            tvBat2Runs = theView.findViewById(R.id.tvBat2RunsScored);
            tvBat2Balls = theView.findViewById(R.id.tvBat2BallsFaced);
            tvBat24s = theView.findViewById(R.id.tvBat24sHit);
            tvBat26s = theView.findViewById(R.id.tvBat26sHit);
            tvBat2SR = theView.findViewById(R.id.tvBat2SR);
        }

		if(ccUtils.getOtherBatsman() != null) {
            trBatsman2.setVisibility(View.VISIBLE);
            tvBat2Name.setText(ccUtils.getOtherBatsman().getBatsmanName());
            tvBat2Runs.setText(String.valueOf(ccUtils.getOtherBatsman().getRunsScored()));
            tvBat2Balls.setText(String.valueOf(ccUtils.getOtherBatsman().getBallsPlayed()));
            tvBat24s.setText(String.valueOf(ccUtils.getOtherBatsman().getNum4s()));
            tvBat26s.setText(String.valueOf(ccUtils.getOtherBatsman().getNum6s()));
            tvBat2SR.setText(CommonUtils.doubleToString(ccUtils.getOtherBatsman().getStrikeRate(), "#.##"));
        } else {
            trBatsman2.setVisibility(View.GONE);
        }

		/* Extras Details*/
        if(isInitial) {
            tvLegByes = theView.findViewById(R.id.tvLegByes);
            tvByes = theView.findViewById(R.id.tvByes);
            tvWides = theView.findViewById(R.id.tvWides);
            tvNoBalls = theView.findViewById(R.id.tvNoBalls);
            tvPenalty = theView.findViewById(R.id.tvPenalty);
        }

		tvLegByes.setText(String.format(getString(R.string.legByes), currCard.getLegByes()));
		tvByes.setText(String.format(getString(R.string.byes), currCard.getByes()));
		tvWides.setText(String.format(getString(R.string.wides), currCard.getWides()));
		tvNoBalls.setText(String.format(getString(R.string.noBalls), currCard.getNoBalls()));
		tvPenalty.setText(currCard.getPenalty() > 0 ? String.format(getString(R.string.penalty), currCard.getPenalty()) : "");

		/* Bowler Details */
        if(isInitial) {
            tvBowlName = theView.findViewById(R.id.tvBowlName);
            tvBowlOvers = theView.findViewById(R.id.tvBowlOvers);
            tvBowlMaidens = theView.findViewById(R.id.tvBowlMaidens);
            tvBowlRuns = theView.findViewById(R.id.tvBowlRuns);
            tvBowlWickets = theView.findViewById(R.id.tvBowlWickets);
            tvBowlEconomy = theView.findViewById(R.id.tvBowlEconomy);
        }

        if(ccUtils.getBowler() != null) {
			tvBowlName.setText(ccUtils.getBowler().getBowlerName());
			tvBowlOvers.setText(ccUtils.getBowler().getOversBowled());
			tvBowlMaidens.setText(String.valueOf(ccUtils.getBowler().getMaidens()));
			tvBowlRuns.setText(String.valueOf(ccUtils.getBowler().getRunsGiven()));
			tvBowlWickets.setText(String.valueOf(ccUtils.getBowler().getWickets()));
			tvBowlEconomy.setText(CommonUtils.doubleToString(ccUtils.getBowler().getEconomy(), "#.##"));
		}

		if(isInitial) {
			tvResult = theView.findViewById(R.id.tvResult);
			tvRunsInBalls = theView.findViewById(R.id.tvRunsInBalls);
		}
		if(currCard.getInnings() == 2) {
        	int runsReq = ccUtils.getCard().getTarget() - ccUtils.getCard().getScore();
        	int ballsRem = CommonUtils.oversToBalls(Double.parseDouble(ccUtils.getCard().getMaxOvers()))
							- CommonUtils.oversToBalls(Double.parseDouble(ccUtils.getCard().getTotalOversBowled()));

        	tvRunsInBalls.setText(String.format(getString(R.string.runsInBalls), runsReq, ballsRem));
		}
	}

	private void newBallBowled(Extra extra, int runs, @Nullable WicketData wicketData) {
		autoSaveMatch();
		ccUtils.processBallActivity(extra, runs, wicketData, false);
		updateCardDetails(false);
        checkChangeOfBowler();
        if(ccUtils.getCard().isInningsComplete()) {
            updateViewToCloseInnings();
        }
	}

	@Override
	public void onClick(View view) {
		newBallBowled(view);
	}

    public void processExtra(Extra.ExtraType extraType, int numExtraRuns, String penaltyFavouringTeam, Extra.ExtraType extraSubType) {
        Extra extra;
        switch (extraType) {
            case PENALTY:
                if(numExtraRuns > 0) {
                    extra = new Extra(Extra.ExtraType.PENALTY, numExtraRuns);
                    ccUtils.addPenalty(extra, penaltyFavouringTeam);
                }
                break;

            case LEG_BYE:
                if(numExtraRuns > 0) {
                    extra = new Extra(Extra.ExtraType.LEG_BYE, numExtraRuns);
                    newBallBowled(extra, 0, null);
                    ccUtils.checkNextBatsmanFacingBall(extra.getRuns());
                }
                break;

            case BYE:
                if(numExtraRuns > 0) {
                    extra = new Extra(Extra.ExtraType.BYE, numExtraRuns);
                    newBallBowled(extra, 0, null);
                    ccUtils.checkNextBatsmanFacingBall(extra.getRuns());
                }
                break;

            case WIDE:
                if(numExtraRuns >= 0) {
                    extra = new Extra(Extra.ExtraType.WIDE, numExtraRuns);
                    newBallBowled(extra, 0, null);
                }
                break;

            case NO_BALL:
                if(numExtraRuns >= 0) {
                    extra = new Extra(Extra.ExtraType.NO_BALL, 0, extraSubType);
                    newBallBowled(extra, numExtraRuns, null);
                }
                break;
        }
    }

	private void newBallBowled(View view) {
		switch (view.getId()) {
			case R.id.btnRuns0:
				newBallBowled(null, 0, null);
				break;

			case R.id.btnRuns1:
				newBallBowled(null, 1, null);
				break;

			case R.id.btnRuns2:
				newBallBowled(null, 2, null);
				break;

			case R.id.btnRuns3:
				newBallBowled(null, 3, null);
				break;

			case R.id.btnRuns4:
				newBallBowled(null, 4, null);
				break;

			case R.id.btnRuns6:
				newBallBowled(null, 6, null);
				break;

			case R.id.btnWicket:
				displayWicketDialog();
				break;

			case R.id.btnExtraPenalty:
				displayExtrasDialog(Extra.ExtraType.PENALTY);
				break;

			case R.id.btnExtrasLegByes:
				displayExtrasDialog(Extra.ExtraType.LEG_BYE);
				break;

			case R.id.btnExtrasByes:
				displayExtrasDialog(Extra.ExtraType.BYE);
				break;

			case R.id.btnExtrasWides:
				displayExtrasDialog(Extra.ExtraType.WIDE);
				break;

			case R.id.btnExtrasNoBall:
				displayExtrasDialog(Extra.ExtraType.NO_BALL);
				break;

            case R.id.btnSelBatsman:
            	selectBatsman();
                break;

            case R.id.btnSelBowler:
                displayBowlerSelect();
                break;

            case R.id.btnSelFacingBatsman:
                displayBatsmanSelect(null, new BatsmanStats[]{ccUtils.getCurrentFacing() , ccUtils.getOtherBatsman()},
						REQ_CODE_CURRENT_FACING_DIALOG, 0);
                break;

			case R.id.btnStartNextInnings:
				startNewInnings();
				break;
		}
	}

	private void selectBatsman() {
		HashMap<Integer, BatsmanStats> batsmen = ccUtils.getCard().getBatsmen();
		BatsmanStats[] batsmenPlayed = new BatsmanStats[batsmen.size()];

		for(int i=0; i<batsmen.size(); i++) {
			batsmenPlayed[i] = batsmen.get(i+1);
		}

		displayBatsmanSelect(ccUtils.getCard().getBattingTeam(), batsmenPlayed, REQ_CODE_BATSMAN_DIALOG, batsmen.size());
	}

	private void displayExtrasDialog(Extra.ExtraType type) {
		Intent dialogIntent = new Intent(getContext(), ExtraDialogActivity.class);
		dialogIntent.putExtra(CommonUtils.ARG_EXTRA_TYPE, type);
		startActivityForResult(dialogIntent, REQ_CODE_EXTRA_DIALOG);
	}

	private void displayWicketDialog() {
		Intent dialogIntent = new Intent(getContext(), WicketDialogActivity.class);

		dialogIntent.putExtra(WicketDialogActivity.ARG_FACING_BATSMAN, ccUtils.getCurrentFacing());
		dialogIntent.putExtra(WicketDialogActivity.ARG_OTHER_BATSMAN, ccUtils.getOtherBatsman());
		dialogIntent.putExtra(WicketDialogActivity.ARG_BOWLER, ccUtils.getBowler());
		dialogIntent.putExtra(WicketDialogActivity.ARG_FIELDING_TEAM, ccUtils.getCard().getBowlingTeam().toArray());

		startActivityForResult(dialogIntent, REQ_CODE_WICKET_DIALOG);
	}

    private void displayBatsmanSelect(@Nullable List<Player> battingTeam, BatsmanStats[] batsmen, int reqCode, int defaultSelIndex) {
        Intent batsmanIntent = new Intent(getContext(), BatsmanSelectActivity.class);

        if(battingTeam != null) {
            batsmanIntent.putExtra(BatsmanSelectActivity.ARG_PLAYER_LIST, battingTeam.toArray());
            batsmanIntent.putExtra(BatsmanSelectActivity.ARG_DEFAULT_SEL_INDEX, defaultSelIndex);
        }
        batsmanIntent.putExtra(BatsmanSelectActivity.ARG_BATSMAN_LIST, batsmen);

        startActivityForResult(batsmanIntent, reqCode);
    }

    private void displayBowlerSelect() {
        HashMap<String, BowlerStats> bowlerMap = ccUtils.getCard().getBowlerMap();
        BowlerStats[] currBowlers = new BowlerStats[bowlerMap.size()];
        Iterator<String> bowlerItr = bowlerMap.keySet().iterator();
        int i=0;
        while(bowlerItr.hasNext()) {
            currBowlers[i++] = bowlerMap.get(bowlerItr.next());
        }

        Intent bowlerIntent = new Intent(getContext(), BowlerSelectActivity.class);

        bowlerIntent.putExtra(BowlerSelectActivity.ARG_PLAYER_LIST, ccUtils.getCard().getBowlingTeam().toArray());
        bowlerIntent.putExtra(BowlerSelectActivity.ARG_BOWLER_LIST, currBowlers);
        bowlerIntent.putExtra(BowlerSelectActivity.ARG_MAX_OVERS_PER_BOWLER, ccUtils.getCard().getMaxPerBowler());
        bowlerIntent.putExtra(BowlerSelectActivity.ARG_NEXT_BOWLER, ccUtils.getNextBowler());
        bowlerIntent.putExtra(BowlerSelectActivity.ARG_PREV_BOWLER, ccUtils.getPrevBowler());

        startActivityForResult(bowlerIntent, REQ_CODE_BOWLER_DIALOG);
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case REQ_CODE_EXTRA_DIALOG:
				if (resultCode == ExtraDialogActivity.RESULT_CODE_OK) {
					Extra.ExtraType extraType = (Extra.ExtraType) data.getSerializableExtra(CommonUtils.ARG_EXTRA_TYPE);
					Extra.ExtraType extraSubType = (Extra.ExtraType) data.getSerializableExtra(ExtraDialogActivity.ARG_NB_EXTRA);
					int extraRuns = data.getIntExtra(ExtraDialogActivity.ARG_EXTRA_RUNS, -1);
					String team = data.getStringExtra(ExtraDialogActivity.ARG_TEAM);

					processExtra(extraType, extraRuns, team, extraSubType);
					updateCardDetails(false);
				}
				break;

			case REQ_CODE_WICKET_DIALOG:
				if(resultCode == WicketDialogActivity.RESP_CODE_OK) {
				    WicketData wktData = (WicketData) data.getSerializableExtra(WicketDialogActivity.ARG_WICKET_DATA);
				    Extra extraData = (Extra) data.getSerializableExtra(WicketDialogActivity.ARG_EXTRA_DATA);
                    outBatsman = (ccUtils.getCurrentFacing().getPlayer().getID() == wktData.getBatsman().getPlayer().getID())
									? ccUtils.getCurrentFacing() : ccUtils.getOtherBatsman();

					newBallBowled(extraData, 0, wktData);

					dismissalType = wktData.getDismissalType();
                    updateScreenForBatsmanSelect(View.GONE, View.VISIBLE, View.GONE);
                    updateCardDetails(false);
				}
				break;

            case REQ_CODE_BATSMAN_DIALOG:
                if(resultCode == BatsmanSelectActivity.RESP_CODE_OK) {
                    newBatsman = (BatsmanStats) data.getSerializableExtra(BatsmanSelectActivity.ARG_SEL_BATSMAN);

                    if(newBatsman != null) {
						ccUtils.newBatsman(newBatsman);

                    	if(dismissalType != null) {
							switch (dismissalType) {
								case RUN_OUT:
								case RETIRED:
								case OBSTRUCTING_FIELD:
								case CAUGHT:
								case TIMED_OUT:
								case HIT_BALL_TWICE:
									updateScreenForBatsmanSelect(View.GONE, View.GONE, View.VISIBLE);
									break;

								default:
									updateScreenForBatsmanSelect(View.VISIBLE, View.GONE, View.GONE);
									break;
							}
							dismissalType = null;
						} else {
                    		if(startInnings) {
								if (ccUtils.getCurrentFacing() != null && ccUtils.getOtherBatsman() != null) {
									updateScreenForBatsmanSelect(View.GONE, View.GONE, View.VISIBLE);
								} else {
									updateScreenForBatsmanSelect(View.GONE, View.VISIBLE, View.GONE);
								}
							}
						}

                        updateCardDetails(false);
                    }
                }
                break;

            case REQ_CODE_CURRENT_FACING_DIALOG:
                if(resultCode == BatsmanSelectActivity.RESP_CODE_OK) {
                    BatsmanStats facingBatsman = (BatsmanStats) data.getSerializableExtra(BatsmanSelectActivity.ARG_SEL_BATSMAN);
                    if(facingBatsman != null && ccUtils.getCurrentFacing().getPosition() != facingBatsman.getPosition()) {
                        ccUtils.updateFacingBatsman(facingBatsman);
                        updateCardDetails(false);
                    }

                    if(startInnings) {
						updateScreenForBatsmanSelect(View.GONE, View.GONE, View.GONE);
						updateScreenForBowlerSelect(View.GONE, View.VISIBLE);
					} else {
						updateScreenForBatsmanSelect(View.VISIBLE, View.GONE, View.GONE);
					}
                }
                break;

            case REQ_CODE_BOWLER_DIALOG:
                if(resultCode == BowlerSelectActivity.RESP_CODE_OK) {
                    BowlerStats nextBowler = (BowlerStats) data.getSerializableExtra(BowlerSelectActivity.ARG_SEL_BOWLER);
                    if(nextBowler != null) {
                        ccUtils.setBowler(nextBowler);
                        updateCardDetails(false);
                    }

                    updateScreenForBowlerSelect(View.VISIBLE, View.GONE);
                    startInnings = false;
                }
                break;

			case REQ_CODE_GET_SAVE_MATCH_NAME:
				if(resultCode == InputActivity.RESP_CODE_OK) {
					String saveMatchName = data.getStringExtra(InputActivity.ARG_INPUT_TEXT);
					saveMatch(saveMatchName);
				}
		}
	}

	private void checkChangeOfBowler() {
	    if(ccUtils.isNewOver()) {
	    	ccUtils.setBowler(null);
            updateScreenForBowlerSelect(View.GONE, View.VISIBLE);
        }
    }

    @Override
    public void onDetach() {
        confirmSaveMatch();
        super.onDetach();
    }

    @Override
        public void onDestroyView() {
		confirmSaveMatch();
        super.onDestroyView();
    }

    private void updateScreenForBatsmanSelect(int scoringButtonsVisibility, int batsmanSelectionVisibility, int currentFacingSelectVisibility) {
        LinearLayout llScoring = theView.findViewById(R.id.llScoring);
        Button btnSelBatsman = theView.findViewById(R.id.btnSelBatsman);
        Button btnSelFacing = theView.findViewById(R.id.btnSelFacingBatsman);
        TextView tvOutBatsmanDetails = theView.findViewById(R.id.tvOutBatsmanDetails);

        llScoring.setVisibility(scoringButtonsVisibility);
        btnSelBatsman.setVisibility(batsmanSelectionVisibility);
        btnSelFacing.setVisibility(currentFacingSelectVisibility);

        if(dismissalType != null)
		{
			int outBatsmanVisibility = (batsmanSelectionVisibility == View.VISIBLE || currentFacingSelectVisibility == View.VISIBLE)
										? View.VISIBLE : View.GONE;
			tvOutBatsmanDetails.setVisibility(outBatsmanVisibility);
			String outBy = "", bowledBy = "", score = outBatsman.getRunsScored() + "(" + outBatsman.getBallsPlayed() + ")";
			switch (dismissalType) {
				case CAUGHT:
					outBy = "c " + ((outBatsman.getWicketEffectedBy().getID() == ccUtils.getBowler().getPlayer().getID())
									? "&" : outBatsman.getWicketEffectedBy().getName());
					bowledBy = "b " + ccUtils.getBowler().getBowlerName();
					break;

				case STUMPED:
					outBy = "st " + outBatsman.getWicketEffectedBy().getName();
					bowledBy = "b " + ccUtils.getBowler().getBowlerName();
					break;

				case RUN_OUT:
					outBy = "runout (" + outBatsman.getWicketEffectedBy().getName() + ")";
					break;

				case BOWLED:
					bowledBy = "b " + ccUtils.getBowler().getBowlerName();
					break;

				case LBW:
					bowledBy = "lbw " + ccUtils.getBowler().getBowlerName();
					break;

				case HIT_BALL_TWICE:
					outBy = "(hit ball twice)";
					break;

				case HIT_WICKET:
					outBy = "(hit-wicket)";
					break;

				case OBSTRUCTING_FIELD:
					outBy = "(obstructing field)";
					break;

				case RETIRED:
					outBy = "(retired)";
					break;

				case TIMED_OUT:
					outBy = "(timed out)";
					break;
			}

			tvOutBatsmanDetails.setText(String.format(Locale.getDefault(), getString(R.string.outBatsmanData),
					outBatsman.getBatsmanName(), outBy, bowledBy, score));
		} else {
			tvOutBatsmanDetails.setVisibility(View.GONE);
		}
    }

	private void updateScreenForBowlerSelect(int scoringButtonsVisibility, int bowlerSelectVisibility) {
        LinearLayout llScoring = theView.findViewById(R.id.llScoring);
        Button btnSelBowler = theView.findViewById(R.id.btnSelBowler);

        llScoring.setVisibility(scoringButtonsVisibility);
        btnSelBowler.setVisibility(bowlerSelectVisibility);
    }

    private void updateScreenForResult() {
		LinearLayout llScoring = theView.findViewById(R.id.llScoring);

		llScoring.setVisibility(View.GONE);
		tvResult.setVisibility(View.VISIBLE);
	}

    private void updateViewToCloseInnings() {
        if(ccUtils.getCard().getInnings() == 1) {
			updateScreenForBatsmanSelect(View.GONE, View.GONE, View.GONE);
			updateScreenForBowlerSelect(View.GONE, View.GONE);
			theView.findViewById(R.id.btnStartNextInnings).setVisibility(View.VISIBLE);
			theView.findViewById(R.id.tvInningsComplete).setVisibility(View.VISIBLE);
		}
        else {
			showResult();
		}
    }

    private void startNewInnings() {
		ccUtils.setNewInnings();
		startInnings = true;
		theView.findViewById(R.id.tvInningsComplete).setVisibility(View.GONE);
		theView.findViewById(R.id.btnStartNextInnings).setVisibility(View.GONE);
		updateCardDetails(false);
		updateScreenForBatsmanSelect(View.GONE, View.VISIBLE, View.GONE);
	}

    private void showResult() {
		theView.findViewById(R.id.tvInningsComplete).setVisibility(View.VISIBLE);
		int score = ccUtils.getCard().getScore();
		int target = ccUtils.getCard().getScore();
		int wicketsFallen = ccUtils.getCard().getWicketsFallen();

		String result;
		if(score >= target) {
			result = String.format(Locale.getDefault(), "%s WON by %d wickets", ccUtils.getTeam2().getName(), (maxWickets - wicketsFallen));
		} else if(score < (target - 1)) {
			result = String.format(Locale.getDefault(), "%s won by %d runs", ccUtils.getTeam1().getName(), (target - 1 - score));
		} else {
			result = "Match TIED";
		}

		updateScreenForResult();
		tvResult.setText(result);
    }

    private void saveMatch(String saveName){
        dbHandler.saveMatchState(matchID, CommonUtils.convertToJSON(ccUtils), saveName);
    }

    private void autoSaveMatch(){
        dbHandler.autoSaveMatch(matchID, CommonUtils.convertToJSON(ccUtils), ccUtils.getMatchName());
        dbHandler.clearMatchStateHistory(6);
    }

    private void confirmSaveMatch() {

	}

	@Override
	public void onConfirmationClick(int confirmationCode, boolean accepted) {
		switch (confirmationCode) {
			case CONFIRMATION_CODE_SAVE_MATCH:
				showInputActivity();
				break;
		}
	}

	private void showSavedMatchDialog() {
		if(getFragmentManager() != null) {
			savedMatchDataList = dbHandler.getSavedMatches(DatabaseHandler.SAVE_MANUAL, matchID, null);
			if (savedMatchDataList != null && savedMatchDataList.size() > 0) {
				List<String> savedMatches = new ArrayList<>();
				for (int i = 0; i < savedMatchDataList.size(); i++)
					savedMatches.add(savedMatchDataList.valueAt(i));

				Collections.sort(savedMatches);

				StringDialog dialog = StringDialog.newInstance("Select Match to Load", (String[]) savedMatches.toArray(), STRING_DIALOG_LOAD_SAVED_MATCHES);
				dialog.setDialogItemClickListener(this);
				dialog.show(getFragmentManager(), "SavedMatchDialog");
			}
		}
	}

	@Override
	public void onItemSelect(String type, String value, int position) {
		switch (type) {
			case STRING_DIALOG_LOAD_SAVED_MATCHES:
				int matchStateID = savedMatchDataList.indexOfValue(value);
				loadMatch(matchStateID);
				dbHandler.clearMatchStateHistory(0);
				break;
		}
	}
}