package wtr.g1;

import wtr.sim.Point;

import java.util.Random;
import java.util.ArrayList;

public class Player implements wtr.sim.Player {

	// your own id
	private int self_id = -1;

	private int soulmate = -1;

	// the remaining wisdom per player
	private int[] W = null;

	// random generator
	private Random random = new Random();
	private ArrayList<Integer> friends;

	ArrayList<Point> nearby_friends;
	ArrayList<Point> nearby_strangers;
	ArrayList<Point> available_friends;
	ArrayList<Point> available_strangers;

	// init function called once
	public void init(int id, int[] friend_ids, int strangers)
	{
		self_id = id;
		friends = new ArrayList<Integer>();
		nearby_friends = new ArrayList<Point>();
		nearby_strangers = new ArrayList<Point>();
		available_friends = new ArrayList<Point>();
		available_strangers = new ArrayList<Point>();

		// initialize the wisdom array
		int N = friend_ids.length + strangers + 2;
		W = new int [N];
		for (int i = 0 ; i != N ; ++i) {
			W[i] = i == self_id ? 0 : -1;
		}
		for (int friend_id : friend_ids) {
			W[friend_id] = 50;
			friends.add(friend_id);
		}
	}

	// play function
	public Point play(Point[] players, int[] chat_ids,
	                  boolean wiser, int more_wisdom)
	{
		// find where you are and who you chat with
		int i = 0, j = 0;
		while (players[i].id != self_id) i++;
		while (players[j].id != chat_ids[i]) j++;
		Point self = players[i];
		Point chat = players[j];

		// record known wisdom
		W[chat.id] = more_wisdom;

		// attempt to continue chatting if there is more wisdom
		if (wiser) {
			return new Point(0.0, 0.0, chat.id);
		}

		nearby_friends.clear();
		nearby_strangers.clear();
		available_friends.clear();
		available_strangers.clear();

		// try to initiate chat if previously not chatting
		if (i == j) {
			for (int k=0; k<players.length; k++) {
				Point p = players[k];
				if (friends.contains(p.id)) {
					nearby_friends.add(p);
					if (chat_ids[k] == p.id) {
						available_friends.add(p);
					}
				} else {
					nearby_strangers.add(p);
					if (chat_ids[k] == p.id) {
						available_strangers.add(p);
					}
				}
			}

			// SM is first priority
			if (soulmate != -1) {
				for (Point p : available_strangers) {
					// skip if not soulmate
					if (W[p.id] != soulmate) {
						continue;
					}
					// skip if soulmate is out of wisdom
					if (W[p.id] > 20 && W[p.id] <= 50) {
						friends.add(p.id);
						soulmate = -1;
						continue;
					}
					if (W[p.id] <= 20) {
						soulmate = -1;
						continue;
					}
					// compute squared distance
					double dx = p.x - self.x;
					double dy = p.y - self.y;
					double dd = dx * dx + dy * dy;
					// start chatting if in range, else move to SM
					if (dd >= 0.25 && dd <= 4.0) {
						return new Point(0.0, 0.0, p.id);
					} else {
						return new Point(dx/1.2, dy/1.2, self_id);
					}
				}
			}

			// find a friend to talk to
			for (Point p : available_friends) {
				// skip if no more wisdom
				if (W[p.id] == 0) {
					continue;
				}
				// compute squared distance
				double dx = p.x - self.x;
				double dy = p.y - self.y;
				double dd = dx * dx + dy * dy;
				// start chatting if in range
				if (dd >= 0.25 && dd <= 4.0)
					return new Point(0.0, 0.0, p.id);
			}

			// find a stranger to talk to
			for (Point p : available_strangers) {
				// skip if no more wisdom to gain
				if (W[p.id] == 0) {
					continue;
				}
				if (W[p.id] > 20) {
					soulmate = p.id;
				}
				// compute squared distance
				double dx = p.x - self.x;
				double dy = p.y - self.y;
				double dd = dx * dx + dy * dy;
				// start chatting if in range
				if (dd >= 0.25 && dd <= 4.0)
					return new Point(0.0, 0.0, p.id);
			}
		}

		// find a friend out of distance, go to that friend
		for (Point p : nearby_friends) {
				// skip if no more wisdom to gain
				if (W[p.id] == 0) {
					continue;
				}
				// compute squared distance
				double dx = p.x - self.x;
				double dy = p.y - self.y;
				double dd = dx * dx + dy * dy;
				// start chatting if in range
				if (dd > 4) {
					return new Point(dx/1.2, dy/1.2, self_id);
				}
			}

		// return a random move
		double dir = random.nextDouble() * 2 * Math.PI;
		double dx = 6 * Math.cos(dir);
		double dy = 6 * Math.sin(dir);
		return new Point(dx, dy, self_id);
	}
}
