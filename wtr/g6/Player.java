package wtr.g6;

import wtr.sim.Point;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Iterator;



public class Player implements wtr.sim.Player {

	// your own id
	private int self_id = -1;

	// the remaining wisdom per player
	private int[] W = null;
    // map of all people in the room
    Map<Integer, Person> people;

	// random generator
	private Random random = new Random();

	PriorityQueue<Person> maximum_wisdom_queue;
	int[] spoken; //0 = not spoken, 1=hello, 2 = zero wisdom left

	int count = 0;

	private boolean exhaust = false;

	// init function called once
	public void init(int id, int[] friend_ids, int strangers)
	{
        people = new HashMap<Integer, Person>();
		self_id = id;
		// initialize the wisdom array
		int N = friend_ids.length + strangers + 2;
		W = new int [N];
		for (int i = 0 ; i != N ; ++i) {
            W[i] = i == self_id ? 0 : -1;
            people.put(i, new Person(i));
        }
		for (int friend_id : friend_ids)
			W[friend_id] = 50;

		spoken = new int[N];
		Arrays.fill(spoken, 0); //0 = not spoken, 1=hello, 2 = zero wisdom left

		maximum_wisdom_queue = new PriorityQueue<Person>(new WisdomComparator());
	}

	// play function
	public Point play(Point[] players, int[] chat_ids,
	                  boolean wiser, int more_wisdom)
	{

		//System.out.println("queue size: " + maximum_wisdom_queue.size());
        updatePeople(players, chat_ids);
		// find where you are and who you chat with
		int i = 0, j = 0;
		while (players[i].id != self_id) i++; // Find myself in the players array.
		while (players[j].id != chat_ids[i]) j++; // Find my chat-buddy (who I'm currently talking to)
		Point self = players[i];
		Point chat = players[j];
		
		W[chat.id] = more_wisdom; // record known wisdom

		//boolean hasPerson = false;		
		for( Person ps : maximum_wisdom_queue)
		{
			if(ps.id == chat.id)
			{
				//hasPerson = true;
				Person tmp = new Person(ps.id, ps.wisdom);
				maximum_wisdom_queue.remove(tmp);
				break;
				//System.out.println("maximum_wisdom_queue.remove(tmp): "+ );
			}
		}
		
//		Person tmp = new Person(chat.id, more_wisdom);
//		boolean hasPerson = maximum_wisdom_queue.remove(tmp);
		
		
//		System.out.println("---------------------------------------");
//		for( Person ps : maximum_wisdom_queue)
//		{
//			System.out.println("ps " + ps.id + ", w: "+ps.wisdom);
//		}
		
		
		maximum_wisdom_queue.add(new Person(chat.id, more_wisdom));
		// System.out.println("Player "+self.id+" now talking to "+chat.id);
		spoken[chat.id] = wiser==true? 1:2; //wiser = more wisdom left
		if(exhaust)
		{
			if(wiser && chat_ids[j] == self.id)
			{
				System.out.println("I, " + self.id + ", am talking to "+chat_ids[j]);
				return new Point(0,0,chat.id);
			}
				
			else {
				exhaust = false;
				// maximum_wisdom_queue.clear();}
			}
		}

		//Say hello!
		if(!exhaust){
		for (Point p : players) {
			// Skip if you've already said hello!
			int idx = 0;
			while (idx<chat_ids.length && chat_ids[idx] != p.id ) idx++;
			if (spoken[p.id] != 0 || idx != self.id || idx != p.id) 
			{
				continue;
			}

			// Say hello if in range & not spoken to earlier!
			if(inTalkRange(self, p))
			{	
				System.out.println(self.id + " trying to saying hello to "+p.id);
				return new Point(0.0, 0.0, p.id);
			}
		}
		}

		
		// exhaust the person with the max wisdom		
		Person[] person_by_w = new Person[maximum_wisdom_queue.size()];
		int pi = 0;
		for(Person ps : maximum_wisdom_queue)
		{
			person_by_w[pi++] = ps;
		}
		Arrays.sort(person_by_w, maximum_wisdom_queue.comparator());
		int k=0;
		for(; k<person_by_w.length; k++)
		{
			int idx = 0;
			// find the actual player with the max wisdom
			while (idx<players.length && players[idx].id != person_by_w[k].id ) idx++;
			// if we can't see the max wisdom person any more or he is out of talking range then pick up the next...
			if(idx >= players.length || !inTalkRange(self, players[idx]))
				continue;
			// else find out who is the max person talking to
			int idx2 = 0;
			while (idx2<chat_ids.length && chat_ids[idx2] != person_by_w[k].id ) idx2++;
			// if he is talking to some one else, then skip
			//System.out.println("idx2: " + idx2 + ", chat_ids.length: "+chat_ids.length+", person_by_w[k].id"+person_by_w[k].id);
			if(idx2 == self.id || idx2 == person_by_w[k].id)
			{
				System.out.println("I, " + self.id + ", am talking to "+person_by_w[k].id);
				exhaust = true;
				return new Point(0.0, 0.0, person_by_w[k].id);
			}
		}
        return moveToANewLocation(players);
	}
	
	private Point moveToANewLocation(Point[] players) {
        Point self = people.get(self_id).cur_position;
        for(int i = 0; i < players.length; i++) {
            Point player = players[i];
            double distance = Utils.distance(self, player);
            // if the person is not in talking range and has wisdom to offer, move to that person's location
            if(distance > 2 && distance <= 6 && W[i] != 0) {
                double theta = Math.atan2(player.y - self.y, player.x - self.x);
                double new_distance = distance - 0.5;
                double dx = Math.abs(new_distance * Math.sin(theta));
                double dy = Math.abs(new_distance * Math.cos(theta));
                if(player.x - self.x < 0) {
                    dx = -dx;
                }
                if(player.y - self.y < 0) {
                    dy = -dy;
                }
                return new Point(dx, dy, self.id);
            }
        }
        // if no one found, move to a random position
        double dir = random.nextDouble() * 2 * Math.PI;
        double dx = 6 * Math.cos(dir);
        double dy = 6 * Math.sin(dir);
        return new Point(dx, dy, self_id);
    }
	
	
	private boolean inTalkRange(Point self, Point p)
	{
		double distance = Utils.distance(self, p);
		if (distance >= 0.5 && distance <= 2.0) return true;
		return false;
	}

    private void updatePeople(Point[] players, int[] chat_ids) {
        for (int i = 0; i < players.length; i++) {
            Point player = players[i];
            int id = player.id;
            Person person = people.get(id);
            person.setNewPosition(player);
            // if position of the person change, that person is moving
            if (Utils.distance(person.prev_position, person.cur_position) != 0) {
                person.setNewStatus(Status.moving);
            } else {
                // if not talking to himself, then talking to someone, otherwise just stayed there
                if(player.id != chat_ids[i]) {
                    person.setNewStatus(Status.talking);
                } else {
                    person.setNewStatus(Status.stayed);
                }
            }
        }
    }


    public void debug_queue(PriorityQueue<Person> maximum_wisdom_queue)
    {
    	if (count++ == 100)
		{	System.out.println("----------------->"+count);
			while(!maximum_wisdom_queue.isEmpty()){
				Person p = maximum_wisdom_queue.poll();
				System.out.println(p.id+" | "+p.wisdom);}
			System.out.println("----------------->"+count);
		}
    }

}
