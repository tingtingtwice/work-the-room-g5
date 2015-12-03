package wtr.g2;

import wtr.sim.Point;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class Player implements wtr.sim.Player {

    // Constants
    public static final int PLAYER_RANGE = 6;
    public static final int PATIENCE_IN_TICS = 5;
    public static final int XTICKS = 4;
    public static final double MIN_RADIUS_FOR_CONVERSATION = 0.505; // slight offset for floating point stuff
    public static final double MAX_RADIUS_FOR_CONVERSATION = 1.995;

    // Static vars
    private static int num_strangers;
    private static int num_friends;
    private static int n; // total number of people
    private static Random random = new Random();

    // Player specific variables
    private int self_id = -1;
    private int time;
    private Person[] people;
    private int last_chatted;
    private int last_time_chatted;
    private int total_wisdom;
    private int total_strangers;
    private int expected_wisdom;

    private Queue<Point> prevLocations;
    private Point locationXTicksAgo;

    private void println(String s) {
        System.out.println(self_id + "\t" +"\t|\t" + s);
    }

    public void init(int id, int[] friend_ids, int strangers) {
        time = 0;
        self_id = id;
        num_strangers = strangers;
        num_friends = friend_ids.length;
        n = num_friends + num_strangers + 2; // people = friends + strangers + soul mate + us
        people = new Person[n];
        total_strangers = strangers+1;
        total_wisdom = 10*strangers + 200;
        prevLocations = new LinkedList<>();
        expected_wisdom = total_wisdom / total_strangers;

        for (int i = 0; i < people.length; i++) {
            Person p = new Person();
            p.status = Person.Status.STRANGER;
            p.id = i;
            p.remaining_wisdom = -1;
            p.wisdom = -1;
            p.has_left = false;
            p.chatted = false;
            people[i] = p;
        }

        Person us = people[self_id];
        us.status = Person.Status.US;
        us.wisdom = 0;
        us.remaining_wisdom = 0;

        for (int friend_id : friend_ids) {
            Person friend = people[friend_id];
            friend.id = friend_id;
            friend.status = Person.Status.FRIEND;
            //TODO: may not need both wisdom and remaining_wisdom
            friend.wisdom = 50;
            friend.remaining_wisdom = 50;
            last_chatted = -1;
        }
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

    private void updateExpectation(int wisdom) { // expected wisdom value of a stranger
        total_strangers--;
        total_wisdom -= wisdom;
        if (total_strangers == 0)
            expected_wisdom = 0;
        else
            expected_wisdom = total_wisdom / total_strangers;
    }

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
        boolean chatting = (i != j);
        boolean prevLocationsFull = (prevLocations.size() == XTICKS);

        if (chat.id != self_id && people[chat.id].remaining_wisdom == -1) {
            if (wiser)
                updateExpectation(more_wisdom + 1);
            else
                updateExpectation(more_wisdom);
        }

        // Find location x ticks ago
        prevLocations.add(self);
        if (prevLocationsFull) {
            locationXTicksAgo = prevLocations.poll();
        }

        if (chatting) {
            if (Utils.dist(self, chat) > MIN_RADIUS_FOR_CONVERSATION) {
                return getCloser(self, chat);
            }
            // attempt to continue chatting if there is more wisdom
            if (wiser) {
                last_chatted = chat.id;
                last_time_chatted = time;
                return new Point(0.0, 0.0, chat.id);
            }
            else { //wait some time before leaving conversation
                if (time - last_time_chatted < PATIENCE_IN_TICS) {
                    return new Point(0.0, 0.0, chat.id);
                }
            }
        }
        else {
            // If player is stationery too long, move
            if (prevLocationsFull && Utils.pointsAreSame(locationXTicksAgo, self)) {
                return randomMove(self, PLAYER_RANGE);
            }
            // See if other player left because we have no wisdom remaining to give
            if (last_chatted != -1 && (people[last_chatted].remaining_wisdom == 9 || people[last_chatted].remaining_wisdom == 19) ) {
                people[last_chatted].has_left = true;
                last_chatted = -1;
            }

            // try to initiate chat if previously not chatting
            // Use a pq to sort potential targets by closest distance
            PriorityQueue<Point> potentialTargets = new PriorityQueue<>(new TargetComparator(self));
            for (Point p : players) {
                if (people[p.id].remaining_wisdom == 0)
                    continue;

                double dis = Math.sqrt(Utils.dist(self, p));
                if (dis <= MAX_RADIUS_FOR_CONVERSATION && dis >= MIN_RADIUS_FOR_CONVERSATION) {
                    potentialTargets.add(p);
                }
            }

            while (!potentialTargets.isEmpty()) {
                Point nextTarget = potentialTargets.poll();
                if (isAvailable(nextTarget.id, players, chat_ids)) {
                    if (!(people[nextTarget.id].remaining_wisdom == 0)){// && !people[nextTarget.id].has_left) {
                        Utils.printChatInitiation(self, nextTarget);
                        return new Point(0.0, 0.0, nextTarget.id);
                    }
                }
                else { // if closest person is not available (is chatting), then he is unlikely to move,
                // so we can't talk to anyone farther away
                    break;
                }
            }

            //Could not find a chat, so plan next move
	    Point bestPlayer = chooseBestPlayer(players, chat_ids);
            if (bestPlayer != null) {
                return moveToOtherPlayer(self, bestPlayer);
            }
        }

        //If all else fails
        return randomMove(self, PLAYER_RANGE);
    }

    private Point chooseBestPlayer(Point[] players, int[] chat_ids) {
        Point bestPlayer = null;
        int maxWisdom = 0;
	Point us = null;
	for (Point p : players) {
	    if (p.id == self_id)
		us = p;
	}
        for (Point p : players) {
            if (p.id == self_id)
                continue;
            int curPlayerRemWisdom = people[p.id].remaining_wisdom;
            if (curPlayerRemWisdom == -1) {
                curPlayerRemWisdom = expected_wisdom;
            }
            if (curPlayerRemWisdom > maxWisdom && !people[p.id].has_left && isAvailable(p.id, players, chat_ids) && emptyRect(us, p, players, chat_ids)) {
                maxWisdom = curPlayerRemWisdom;
                bestPlayer = p;
            }
        }
        return bestPlayer;
    }

    // check if point c is inside the rectangle given by a and b
    private boolean insideRect(Point a, Point b, Point c) {
	return (Math.abs(a.x - c.x) + Math.abs(c.x - b.x) == Math.abs(a.x - b.x) && Math.abs(a.y - c.y) + Math.abs(c.y - b.y) == Math.abs(a.y - b.y));
    }

    // check if the path to a target player is unobstructed
    private boolean emptyRect(Point a, Point b, Point[] players, int[] chat_ids) {
	Point median = new Point(0.5*a.x + 0.5*b.x, 0.5*a.y + 0.5*b.y, 0);
	for (int i=0; i<players.length; i++) {
	    if (players[i].id == a.id || players[i].id == b.id) {continue;}
	    if (insideRect(a, b, players[i]) && isAvailable(players[i].id, players, chat_ids) || Utils.dist(median, players[i]) < 2*MIN_RADIUS_FOR_CONVERSATION) {
		return false;
	    }
	}
	return true;
    }

    private Point moveToOtherPlayer(Point us, Point them) {
        double dis = Utils.dist(us, them)/2 - MIN_RADIUS_FOR_CONVERSATION/2;
        double dx = them.x - us.x;
        double dy = them.y - us.y;
        double theta = Math.atan2(dy, dx);
        return new Point(dis * Math.cos(theta), dis * Math.sin(theta), self_id);
    }

    private Point randomMove(Point self, int maxDist) {
        double dir, dx, dy;
        Point rand;
        do {
            dir = random.nextDouble() * 2 * Math.PI;
            dx = maxDist * Math.cos(dir);
            dy = maxDist * Math.sin(dir);
            rand = new Point(dx, dy, self_id);
        } while (Utils.pointOutOfRange(self, dx, dy));
        return rand;
    }

    //From g5. Throws exceptions once in a while for some reason // probably because the player with id chat_ids[i] is not in visible range
    private boolean isAvailable(int id, Point[] players, int[] chat_ids){
        int i = 0, j = 0;
        try {
            while (players[i].id != id)
                i++;
            while (players[j].id != chat_ids[i])
                j++;
        }
        catch (IndexOutOfBoundsException e) {
            return false;
        }
        return i == j;
    }

    public Point getCloser(Point self, Point target){
        //can't set to 0.5, if 0.5 the result distance may be 0.49
        double targetDis = MIN_RADIUS_FOR_CONVERSATION;
        double dis = Utils.dist(self, target);
        double x = (dis - targetDis) * (target.x - self.x) / dis;
        double y = (dis - targetDis) * (target.y - self.y) / dis;
//        System.out.println("self pos: " + self.x + ", " + self.y);
//        System.out.println("target pos: " + target.x + ", " + target.y);
//        System.out.println("move pos: " + x + ", " + y);
        return new Point(x, y, self_id);
    }
}
