package wtr.g2;

import wtr.sim.Point;

import java.util.Random;

public class Player implements wtr.sim.Player {

    // Constants
    public static final int PLAYER_RANGE = 6;
    public static final double MIN_RADIUS_FOR_CONVERSATION = 0.5;

    // Static vars
    private static int num_strangers;
    private static int num_friends;
    private static int n; // total number of people
    private static Random random = new Random();

    // Play specific variables
    private int self_id = -1;
    private int time;
    private Person[] people;
    private boolean stationaryLastTurn;
    private Point prevPos;
    private int last_chatted;
    private double expected_wisdom;

    public void init(int id, int[] friend_ids, int strangers) {
	time = 0;
	self_id = id;
	stationaryLastTurn = true;
	num_strangers = strangers;
	num_friends = friend_ids.length;
	n = num_friends + num_strangers + 2; // people = friends + strangers + soul mate + us
	people = new Person[n];
	for (int i = 0; i < people.length; i++) {
	    Person p = new Person();
	    p.status = Person.Status.STRANGER;
	    p.id = i;
	    p.remaining_wisdom = -1;
	    p.wisdom = -1;
	    p.has_left = false;
	    people[i] = p;			
	}
		
	Person us = people[self_id];
	us.status = Person.Status.US;
	us.wisdom = 0;

	for (int friend_id : friend_ids) {
	    Person friend = people[friend_id];
	    friend.id = friend_id;
	    friend.status = Person.Status.FRIEND;
	    //TODO: may not need both wisdom and remaining_wisdom
	    friend.wisdom = 50;
	    friend.remaining_wisdom = 50;
	    last_chatted = -1;
	}
	expected_wisdom = 10;
    }

    public boolean blocked(Point[] players, int target_id, double threshold) {
        for (int i=0; i<players.length; i++) {
            if (players[i].id == target_id) {continue;}
            if ( Math.pow(players[i].x - players[target_id].x,2) + Math.pow(players[i].y - players[target_id].y,2) < threshold*threshold) {
                return true;
            }
        }
        return false;
    }

    // play function
    public Point play(Point[] players, int[] chat_ids, boolean wiser, int more_wisdom) {
        time++;
        // find where you are and who you chat with
        int i = 0, j = 0;
        while (players[i].id != self_id)
            i++;
        while (players[j].id != chat_ids[i])
            j++;
        Point self = players[i];
        Point chat = players[j];
        people[chat.id].remaining_wisdom = more_wisdom;
        //		System.out.println(chat.id + " to " + self.id + " has rem wisdom " + more_wisdom);

        // attempt to continue chatting if there is more wisdom
        if (wiser) {
            last_chatted = chat.id;
            return new Point(0.0, 0.0, chat.id);
        }
        else if (last_chatted != -1 && (people[last_chatted].remaining_wisdom == 9 || people[last_chatted].remaining_wisdom == 19) ) {
            people[last_chatted].has_left = true;
            last_chatted = -1;
        }
        if (time % 3 == 0) {
            if (prevPos != null && prevPos.x == self.x && prevPos.y == self.y) {
                // System.out.println("Player has been still too long. Make him move");
                return randomMove(PLAYER_RANGE);
            }
            prevPos = self;
        }

        // try to initiate chat if previously not chatting
        if (i == j) {
	    double closest_dist = 2.0;
	    Point closest_player = null;;
            for (Point p : players) {
                // compute squared distance
                double dis = Math.sqrt(Utils.dist(self, p));
		if (dis < closest_dist && dis < 2) {
		    closest_dist = dis;
		    closest_player = p;
                }
            }
	    if (0.5 < closest_dist && closest_dist < 2.0 && people[closest_player.id].remaining_wisdom > 0) {
		return new Point(0,0,closest_player.id);
	    }
        }

        //Could not find a chat, so plan next move
        int maxWisdom = 0;
        Point bestPlayer = null;
        for (Point p : players) {
            int curPlayerRemWisdom = people[p.id].remaining_wisdom;
            if (curPlayerRemWisdom > maxWisdom && !people[p.id].has_left) {
                maxWisdom = curPlayerRemWisdom;
                bestPlayer = p;
            }
        }

        if (bestPlayer != null) {
            // Move towards target player's known position
            System.out.println(self.id + " moving towards: " + bestPlayer.id);
            return moveToOtherPlayer(self, bestPlayer);
        }

        //else alternate between staying still and random move
        //		if (stationaryLastTurn) {
        return randomMove(PLAYER_RANGE);
        //		}
        //		else {
        //			return randomMove(0);
        //		}
        //		stationaryLastTurn = !stationaryLastTurn;
    }

    private Point moveToOtherPlayer(Point us, Point them) {
        double dis = Utils.dist(us, them)/2 - MIN_RADIUS_FOR_CONVERSATION;
        double dx = them.x - us.x;
        double dy = them.y - us.y;
        double theta = Math.atan2(dy, dx);
        return new Point(us.x + dis*Math.cos(theta), us.y + dis*Math.sin(theta), self_id);
    }

    public Point randomMove(int maxDist) {
        stationaryLastTurn = !stationaryLastTurn;
        double dir = random.nextDouble() * 2 * Math.PI;
        double dx = maxDist * Math.cos(dir);
        double dy = maxDist * Math.sin(dir);
        System.out.println("Random move");
        return new Point(dx, dy, self_id);
    }
}
