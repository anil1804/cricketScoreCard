package com.thenewcone.myscorecard.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.thenewcone.myscorecard.R;
import com.thenewcone.myscorecard.adapter.BatsmanListAdapter;
import com.thenewcone.myscorecard.intf.ItemClickListener;
import com.thenewcone.myscorecard.player.BatsmanStats;
import com.thenewcone.myscorecard.player.Player;
import com.thenewcone.myscorecard.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatsmanSelectActivity extends Activity
        implements ItemClickListener, View.OnClickListener {

    public static final String ARG_PLAYER_LIST = "PlayerList";
    public static final String ARG_BATSMAN_LIST = "BatsmanList";
    public static final String ARG_SEL_BATSMAN = "SelectedBatsman";
    public static final String ARG_DEFAULT_SEL_INDEX = "DefaultIndex";

    public static final int RESP_CODE_OK = 1;
    public static final int RESP_CODE_CANCEL = -1;

    List<BatsmanStats> dispBatsmen;
    int currBattingPosn= -1;
    @Override
    public void onBackPressed() {
        //
    }

    BatsmanStats[] batsmen;
    Player[] players;
    BatsmanStats selBatsman;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_select);

        RecyclerView rcvPlayerList = findViewById(R.id.rcvPlayerList);
        rcvPlayerList.setHasFixedSize(false);

        findViewById(R.id.btnSelPlayerOK).setOnClickListener(this);
        findViewById(R.id.btnSelPlayerCancel).setOnClickListener(this);

        Intent intent = getIntent();
        int defaultSelectIx = -1;
        if(intent != null) {
            players = CommonUtils.objectArrToPlayerArr((Object[]) intent.getSerializableExtra(ARG_PLAYER_LIST));
            batsmen = CommonUtils.objectArrToBatsmanArr((Object[]) intent.getSerializableExtra(ARG_BATSMAN_LIST));
            defaultSelectIx = intent.getIntExtra(ARG_DEFAULT_SEL_INDEX, -1);

            dispBatsmen = getDisplayBatsmanList(players, batsmen);
        }

        if(dispBatsmen != null && dispBatsmen.size() > 0) {
            rcvPlayerList.setLayoutManager(new LinearLayoutManager(this));
            BatsmanListAdapter adapter = new BatsmanListAdapter(this, dispBatsmen, currBattingPosn, defaultSelectIx);
            adapter.setClickListener(this);
            rcvPlayerList.setAdapter(adapter);

            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            rcvPlayerList.setLayoutManager(llm);

            DividerItemDecoration itemDecor = new DividerItemDecoration(rcvPlayerList.getContext(), DividerItemDecoration.HORIZONTAL);
            rcvPlayerList.addItemDecoration(itemDecor);

            rcvPlayerList.setItemAnimator(new DefaultItemAnimator());
        }
    }

    private List<BatsmanStats> getDisplayBatsmanList(Player[] players, BatsmanStats[] batsmenPlayed) {
        List<BatsmanStats> dispBatsmen = new ArrayList<>();

        dispBatsmen.addAll(Arrays.asList(batsmenPlayed));

        List<String> playedBatsmen;
        if(players != null && players.length > 0) {
            playedBatsmen = getPlayedBatsmen(batsmenPlayed);
            currBattingPosn = playedBatsmen.size() + 1;

            for(Player player : players) {
                if(!playedBatsmen.contains(player.getName())) {
                    dispBatsmen.add(new BatsmanStats(player, currBattingPosn));
                }
            }
        } else {
            currBattingPosn = -1;
        }

        return dispBatsmen;
    }

    private List<String> getPlayedBatsmen(BatsmanStats[] batsmenPlayed) {
        List<String> playedPlayers = new ArrayList<>();

        for(BatsmanStats batsman : batsmenPlayed)
            playedPlayers.add(batsman.getBatsmanName());

        return playedPlayers;
    }

    @Override
    public void onClick(View view) {
        Intent respIntent = getIntent();

        switch (view.getId()) {
            case R.id.btnSelPlayerOK:
                if(selBatsman == null)
                    Toast.makeText(this, "Batsman not selected", Toast.LENGTH_SHORT).show();
                else {
                    respIntent.putExtra(ARG_SEL_BATSMAN, selBatsman);
                    setResult(RESP_CODE_OK, respIntent);
                    finish();
                }
                break;

            case R.id.btnSelPlayerCancel:
                setResult(RESP_CODE_CANCEL);
                finish();
                break;

        }
    }

    @Override
    public void onItemClick(View view, int position) {
        selBatsman = dispBatsmen.get(position);
    }
}