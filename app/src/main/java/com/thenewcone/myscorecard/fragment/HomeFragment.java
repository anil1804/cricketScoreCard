package com.thenewcone.myscorecard.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thenewcone.myscorecard.R;

public class HomeFragment extends Fragment implements View.OnClickListener{

	public static HomeFragment newInstance() {
		return new HomeFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
        View theView;
		theView = inflater.inflate(R.layout.home_fragment, container, false);

		theView.findViewById(R.id.btnLimitedOvers).setOnClickListener(this);
		theView.findViewById(R.id.btnManagePlayer).setOnClickListener(this);

		return theView;
	}

	@Override
	public void onClick(View view) {
		if(getActivity() != null) {
			FragmentManager fragMgr = getActivity().getSupportFragmentManager();
            String fragmentTag;

			switch(view.getId()) {
				case R.id.btnLimitedOvers :
                    fragmentTag = LimitedOversFragment.class.getSimpleName();
					fragMgr.beginTransaction()
							.replace(R.id.frame_container, LimitedOversFragment.newInstance(), fragmentTag)
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

			}
		}
	}
}
