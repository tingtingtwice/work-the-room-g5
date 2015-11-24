package wtr.g5;

import wtr.sim.Point;

import java.util.HashSet;
import java.util.Random;

public class Player implements wtr.sim.Player {

	// your own id
	private int self_id = -1;

	// the remaining wisdom per player
	private int[] W = null;

	// random generator
	private Random random = new Random();
	
	private HashSet<Integer> friendSet;
	
	private int interfereThreshold = 5;
	
	private int interfereCount = 0;
	private Integer preChatId;
	// init function called once
	public void init(int id, int[] friend_ids, int strangers)
	{
		self_id = id;
		friendSet = new HashSet<Integer>();
		// initialize the wisdom array
		int N = friend_ids.length + strangers + 2;
		W = new int [N];
		// initialize strangers' wisdom to 5.5 (avg wisdom for 1/3 + 1/3 + 1/3 configuration)
		for (int i = 0 ; i != N ; ++i)
			W[i] = i == self_id ? 0 : 5;
		for (int friend_id : friend_ids){
			friendSet.add(friend_id);
			W[friend_id] = 50;
		}
		preChatId = self_id;
	}

	// play function
	public Point play(Point[] players, int[] chat_ids, boolean wiser, int more_wisdom)
	{
		if(friendSet.contains(self_id))
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!");
		// find where you are and who you chat with
		// for(int i = 0; i < players.length; ++i) {
		// 	if(players[i].id != i)
		// 		System.out.println("ID NOT I: "+players[i].id+"\t"+i);
		// }
		int i = 0, j = 0;
		while (players[i].id != self_id) i++;
		while (players[j].id != chat_ids[i]) j++;
		Point self = players[i];
		Point chat = players[j];
		//soul mate
		if(more_wisdom > 50)
			friendSet.add(chat.id);
		// record known wisdom
		W[chat.id] = more_wisdom;
		//TODO remove from blacklist
		// attempt to continue chatting if there is more wisdom
		// System.out.println("wise: " + wiser + " selfid " + self_id + " chatid " + chat.id + " W " + W[chat.id]);
		if(chat.id != preChatId)
			interfereCount = 0;
		if(!wiser && (friendSet.contains(chat.id) && W[chat.id] > 0)) {
			interfereCount++;
		}
		if (wiser || (friendSet.contains(chat.id) && W[chat.id] > 0)) {
			if(!wiser && interfereCount >= interfereThreshold){
				//If two friends has been interfered more than 5 times, then move away
				return randomMoveInRoom(self);
			}else{
				preChatId = chat.id;
				return new Point(0.0, 0.0, chat.id);
			}
		}
		// try to initiate chat if previously not chatting
		if (i == j)
			for (Point p : players) {

				Point thisPlayer = pickTarget(players, 6, chat_ids);
				if (thisPlayer == null) {
					System.out.println("no valid target.");
				} else {
					System.out.println("thisPlayer.id" + thisPlayer.id);
				}
				
				System.exit(0);
				
				// System.out.println("pid: " + p.id + " W: " + W[p.id] + " self: " + self_id);
				// skip if no more wisdom to gain
				if (W[p.id] == 0 ) continue;
				// compute squared distance
				double dx = self.x - p.x;
				double dy = self.y - p.y;
				double dd = dx * dx + dy * dy;
				// start chatting if in range
				if (dd >= 0.25 && dd <= 4.0){
					// System.out.println("Self " + self_id + " attempt " + p.id + " W " + W[p.id]);
					if(isAlone(p.id, players, chat_ids)){
						preChatId = p.id;
						return new Point(0.0, 0.0, p.id);
					}
				}

			}
		// return a random move
		return randomMoveInRoom(self);
	}
	
	public Point randomMoveInRoom(Point current) {
		Point move = randomMove();
		while(move.x + current.x > 20 || move.y + current.y > 20 || move.x + current.x < 0 || move.y + current.y < 0) {
			move = randomMove();
		}
		// System.out.println("Self " + self_id + " Moving");
		return move;
	}
	
	private Point randomMove(){
		double dir = random.nextDouble() * 2 * Math.PI;
		double dx = 6 * Math.cos(dir);
		double dy = 6 * Math.sin(dir);
		preChatId = self_id;
		return new Point(dx, dy, self_id);
	}
	
	public boolean isAlone(Integer id, Point[] players, int[] chat_ids){
		int i = 0, j = 0;
		while (players[i].id != id) i++;
		while (players[j].id != chat_ids[i]) j++;
		return i == j;
		
	}

	public Point pickTarget(Point[] players, double distance, int[] chat_ids){
		int maxWisdom = 0;
		Point maxTarget = null;

		if (distance > 6.0) 
			distance = 6.0;

		for (int i = 0; i < players.length; i++){

			// not conversing with anyone
			if (players[i].id != chat_ids[i])
				continue;
			// swap with maxWisdom and maxTarget if wiser
			// System.out.println("this wisdom: " + W[players[i].id]);
			if (W[players[i].id] > maxWisdom) {
				
				maxWisdom = W[players[i].id];
				maxTarget = players[i];
			}
			System.out.println("max wisdom: " + maxWisdom);
		}

		
		return maxTarget;
	}

	public Point getCloser(Point self, Point target){
		//can't set to 0.5, if 0.5 the result distance may be 0.49
		double targetDis = 0.6;
		double dis = distance(self, target);
		double x = (dis - targetDis) * (target.x - self.x) / dis + self.x;
		double y = (dis - targetDis) * (target.y - self.y) / dis + self.y;
		return new Point(x, y, target.id);
	}
	public double distance(Point p1, Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
}
