package com.theNewCone.cricketScoreCard.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.theNewCone.cricketScoreCard.R;
import com.theNewCone.cricketScoreCard.tournament.Group;

import java.util.List;

public class PointsTableViewAdapter extends RecyclerView.Adapter<PointsTableViewAdapter.ViewHolder> {

	private final List<Group> groupList;
	private final Context context;

	public PointsTableViewAdapter(Context context, List<Group> groupList) {
		this.context = context;
		this.groupList = groupList;
	}

	@Override
	@NonNull
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

		View view = LayoutInflater.from(parent.getContext()).
				inflate(R.layout.view_tournament_group_item, parent, false);

		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
		final int adapterPosition = holder.getAdapterPosition();
		holder.group = groupList.get(adapterPosition);

		holder.tvGroupName.setText(holder.group.getName());

		holder.rcvGroupTeamList.setLayoutManager(new LinearLayoutManager(context));
		holder.rcvGroupTeamList.setHasFixedSize(false);

		PointsDataViewAdapter adapter = new PointsDataViewAdapter(context, holder.group.getPointsData());
		holder.rcvGroupTeamList.setAdapter(adapter);

		LinearLayoutManager llm = new LinearLayoutManager(context);
		llm.setOrientation(LinearLayoutManager.VERTICAL);
		holder.rcvGroupTeamList.setLayoutManager(llm);

		holder.rcvGroupTeamList.setItemAnimator(new DefaultItemAnimator());
	}

	@Override
	public int getItemCount() {
		return groupList.size();
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		final View mView;
		final TextView tvGroupName;
		final RecyclerView rcvGroupTeamList;
		Group group;

		ViewHolder(View view) {
			super(view);
			mView = view;

			tvGroupName = view.findViewById(R.id.tvGroupName);
			rcvGroupTeamList = view.findViewById(R.id.rcvGroupList);
		}
	}
}
