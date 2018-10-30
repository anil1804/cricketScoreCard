package com.thenewcone.myscorecard.match;

import com.thenewcone.myscorecard.player.Player;

import java.io.Serializable;
import java.util.List;

public class Team implements Serializable {
    private int id;
    private String name, shortName;
    private List<Player> matchPlayers;
    private Player captain, wicketKeeper;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setId(int id) {
        this.id = id;
    }

	public List<Player> getMatchPlayers() {
		return matchPlayers;
	}

	public void setMatchPlayers(List<Player> matchPlayers) {
		this.matchPlayers = matchPlayers;
	}

	public Player getCaptain() {
		return captain;
	}

	public void setCaptain(Player captain) {
		this.captain = captain;
	}

	public Player getWicketKeeper() {
		return wicketKeeper;
	}

	public void setWicketKeeper(Player wicketKeeper) {
		this.wicketKeeper = wicketKeeper;
	}

	public boolean isCaptain(Player player) {
    	return (captain != null && (captain.getID() == player.getID()));
	}

	public boolean isWK(Player player) {
    	return (wicketKeeper != null && (wicketKeeper.getID() == player.getID()));
	}

	public Team(int id, String shortName) {
        this.id = id;
        this.shortName = shortName;
    }

    public Team(String name, String shortName) {
    	this.id = -1;
        this.name = name;
        this.shortName = shortName;
    }

    public Team(int id, String name, String shortName) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
    }
}
