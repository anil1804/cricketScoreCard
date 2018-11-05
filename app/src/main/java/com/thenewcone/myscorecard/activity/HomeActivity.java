package com.thenewcone.myscorecard.activity;

import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.thenewcone.myscorecard.R;
import com.thenewcone.myscorecard.fragment.HomeFragment;
import com.thenewcone.myscorecard.fragment.NewMatchFragment;
import com.thenewcone.myscorecard.fragment.PlayerFragment;
import com.thenewcone.myscorecard.fragment.TeamFragment;
import com.thenewcone.myscorecard.intf.DrawerLocker;

import java.util.HashMap;
import java.util.List;

public class HomeActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener, DrawerLocker {

	private static final String FRAGMENT = "Fragment";
	private static final String FRAGMENT_TAG = "FragmentTag";
	DrawerLayout drawer;
	ActionBarDrawerToggle toggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_activity);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.frame_container, HomeFragment.newInstance())
					.commitNow();
		}

		setupDrawer();
	}

	void setupDrawer() {
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		drawer = findViewById(R.id.drawer_layout);
		toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

		updateDrawerEnabled(true);

		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			if(getSupportFragmentManager().getBackStackEntryCount() > 0)
				getSupportFragmentManager().popBackStack();
			else {
				if (isFragmentVisible(HomeFragment.class.getSimpleName())) {
					finish();
				} else {
					super.onBackPressed();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		HashMap<String, Object> fragDtlMap = getFragmentDetails(item);

		if(fragDtlMap.size()  == 2) {
			Fragment fragment = (Fragment) fragDtlMap.get(FRAGMENT);
			String fragmentTag = (String) fragDtlMap.get(FRAGMENT_TAG);

			if(fragment !=  null && fragmentTag != null) {
				getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_container, fragment, fragmentTag)
                        .addToBackStack(fragmentTag)
                        .commit();
			}
		}

		DrawerLayout drawer =  findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		return true;
	}

	@Override
	public void setDrawerEnabled(boolean enabled) {
		updateDrawerEnabled(enabled);
	}

	public void updateDrawerEnabled(boolean enabled) {
		int lockMode = enabled ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
		drawer.setDrawerLockMode(lockMode);
		toggle.setDrawerIndicatorEnabled(enabled);
	}

	private HashMap<String, Object> getFragmentDetails(@NonNull MenuItem item) {
		HashMap<String, Object> respMap = new HashMap<>();

		switch (item.getItemId()) {
			case R.id.nav_home:
				if(!isFragmentVisible(HomeFragment.class.getSimpleName())) {
					respMap.put(FRAGMENT, HomeFragment.newInstance());
					respMap.put(FRAGMENT_TAG, HomeFragment.class.getSimpleName());
				}
				break;

			case R.id.nav_manage_player:
				if(!isFragmentVisible(PlayerFragment.class.getSimpleName())) {
					respMap.put(FRAGMENT, PlayerFragment.newInstance());
					respMap.put(FRAGMENT_TAG, PlayerFragment.class.getSimpleName());
				}
				break;

			case R.id.nav_manage_team:
				if(!isFragmentVisible(TeamFragment.class.getSimpleName())) {
					respMap.put(FRAGMENT, TeamFragment.newInstance());
					respMap.put(FRAGMENT_TAG, TeamFragment.class.getSimpleName());
				}
				break;

			case R.id.nav_new_match:
				if(!isFragmentVisible(NewMatchFragment.class.getSimpleName())) {
					respMap.put(FRAGMENT, NewMatchFragment.newInstance());
					respMap.put(FRAGMENT_TAG, NewMatchFragment.class.getSimpleName());
				}
				break;
		}

		return respMap;
	}

	private boolean isFragmentVisible(@NonNull String fragmentTag) {
		List<Fragment> fragList = getSupportFragmentManager().getFragments();

		for(Fragment frag : fragList) {
			if(frag != null && frag.isVisible() && fragmentTag.equals(frag.getTag()))
				return true;
		}

		return false;
	}
}
