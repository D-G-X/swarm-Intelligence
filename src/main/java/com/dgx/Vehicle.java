package com.dgx;

import java.util.ArrayList;

public class Vehicle {
	static int allId = 0;
	// Tunable avoidance constants (adjustable at runtime via UI)
	public static double BASE_AVOIDANCE_RADIUS = 19.0; // world units
	public static double AVOIDANCE_MULTIPLIER = 4.0;   // intensity scaling
	public static double OBS_WEIGHT = 0.25;            // weight used when combining forces
	public static double F_ZUS_WEIGHT = 0.05;
	public static double F_SEP_WEIGHT = 0.55;
	public static double F_AUS_WEIGHT = 0.4;
	int id; 
	double rad_sep; 
	double rad_zus; 
	int type;


	final double FZL; 
	final double FZB; 
	
	double[] pos; 
	double[] vel; 
	
	final double max_acc; 
	final double max_vel; 

	Vehicle() {
		allId++;
		this.id = allId;
		this.FZL = 3;
		this.FZB = 1.5;
		this.rad_sep = 5;
		this.rad_zus = 25;
		this.max_acc = 0.2;
		this.max_vel = 1;

		pos = new double[2];
		vel = new double[2];
		
		pos[0] = Simulation.pix * 800 * Math.random();
		pos[1] = Simulation.pix * 800 * Math.random();
		double angle = 2 * Math.PI * Math.random();
		vel[0] = Math.cos(angle) * max_vel;
		vel[1] = Math.sin(angle) * max_vel;
	}

	ArrayList<Vehicle> neighbours(ArrayList<Vehicle> all, double radius1, double radius2) {
		ArrayList<Vehicle> neighbours = new ArrayList<Vehicle>();
		for (int i = 0; i < all.size(); i++) {
			Vehicle v = all.get(i);
			if (v.id != this.id) {
				double dist = Math.sqrt(Math.pow(v.pos[0] - this.pos[0], 2) + Math.pow(v.pos[1] - this.pos[1], 2));
				if (dist >= radius1 && dist < radius2) {
					neighbours.add(v);
				}
			}
		}
		return neighbours;
	}

	double[] calculateAcc(double[] vel_dest) {
		double[] acc_dest = new double[2];

		if (VectorCalculation.length(vel_dest) > 1e-8) {
		    vel_dest = VectorCalculation.normalize(vel_dest);
		}
		
		vel_dest[0] = vel_dest[0] * max_vel;
		vel_dest[1] = vel_dest[1] * max_vel;

		acc_dest[0] = vel_dest[0] - vel[0];
		acc_dest[1] = vel_dest[1] - vel[1];

		return acc_dest;
	}

	double[] cohesion(ArrayList<Vehicle> all) {
		ArrayList<Vehicle> neighbours;
		
		double[] pos_dest = new double[2];
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		neighbours = neighbours(all, rad_sep, rad_zus);

		if (neighbours.size() > 0) {
			pos_dest[0] = 0;
			pos_dest[1] = 0;
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v = neighbours.get(i);
				pos_dest[0] = pos_dest[0] + v.pos[0];
				pos_dest[1] = pos_dest[1] + v.pos[1];
			}
			pos_dest[0] = pos_dest[0] / neighbours.size();
			pos_dest[1] = pos_dest[1] / neighbours.size();

			vel_dest[0] = pos_dest[0] - pos[0];
			vel_dest[1] = pos_dest[1] - pos[1];

			acc_dest = calculateAcc(vel_dest);
			acc_dest = VectorCalculation.truncate(acc_dest, max_acc);

		}
		return acc_dest;
	}

	double[] separation(ArrayList<Vehicle> all) {
		ArrayList<Vehicle> neighbours;
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		neighbours  = neighbours(all, 0, rad_sep);

		if (neighbours.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v    = neighbours.get(i);
				double[] vel = new double[2];
				double dist;

				vel[0] = v.pos[0] - pos[0];
				vel[1] = v.pos[1] - pos[1];
				
				dist   = rad_sep  - VectorCalculation.length(vel);
				if (dist < 0)System.out.println("mistake in rad");
			
				if (VectorCalculation.length(vel) > 1e-8) {
				    vel = VectorCalculation.normalize(vel);
				}
				
				vel[0] = -vel[0] * dist;
				vel[1] = -vel[1] * dist;
				
				vel_dest[0] = vel_dest[0] + vel[0];
				vel_dest[1] = vel_dest[1] + vel[1];
			}

			acc_dest = calculateAcc(vel_dest);
			acc_dest = VectorCalculation.truncate(acc_dest, max_acc);

		}

		return acc_dest;
	}

	double[] alignment(ArrayList<Vehicle> all) {
		ArrayList<Vehicle> neighbours = new ArrayList<Vehicle>();
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];
		acc_dest[0] = 0;
		acc_dest[1] = 0;

		neighbours = neighbours(all, 0, rad_zus);


		if (neighbours.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v = neighbours.get(i);
				vel_dest[0] = vel_dest[0] + v.vel[0];
				vel_dest[1] = vel_dest[1] + v.vel[1];
			}
			vel_dest[0] = vel_dest[0] / neighbours.size();
			vel_dest[1] = vel_dest[1] / neighbours.size();

			
			acc_dest = calculateAcc(vel_dest);
			acc_dest = VectorCalculation.truncate(acc_dest, max_acc);

		}

		return acc_dest;
	}

    public double[] calculateWeightedAcc1(ArrayList<Vehicle> allVehicles, ArrayList<Obstacle> obstacles, double[] target) {
        double[] acc_dest;
        double[] acc_swarm = new double[2]; // sum of cohesion, separation, alignment[cite: 1]
		double f_zus = F_ZUS_WEIGHT;
		double f_sep = F_SEP_WEIGHT;
		double f_aus = F_AUS_WEIGHT;
		double f_obs = OBS_WEIGHT; // High priority to avoid hitting boxes
        double f_target = 0.3;

        double[] acc_cohesion = cohesion(allVehicles);
        double[] acc_sep = separation(allVehicles);
        double[] acc_align = alignment(allVehicles);
        double[] acc_obs = obstacleAvoidance(obstacles);
        double[] acc_seek = seekTarget(target);


        double x = (f_zus * acc_cohesion[0]) + (f_sep * acc_sep[0]) +
                (f_aus * acc_align[0]) + (f_obs * acc_obs[0]);
        double y = (f_zus * acc_cohesion[1]) + (f_sep * acc_sep[1]) +
                (f_aus * acc_align[1]) + (f_obs * acc_obs[1]);

        acc_dest = new double[]{x, y};
        acc_dest = VectorCalculation.truncate(acc_dest, max_acc);
        return acc_dest;
    }

    public double[] calculateWeightedAcc(ArrayList<Vehicle> allVehicles, ArrayList<Obstacle> obstacles, double[] target, boolean isConsuming) {
        // 1. Define all weights
		double f_zus = F_ZUS_WEIGHT;
		double f_sep = F_SEP_WEIGHT;
		double f_obs = isConsuming ? Math.max(OBS_WEIGHT, 1.2) : OBS_WEIGHT; // Keep obstacle avoidance strong while consuming
//        double f_target = 0.3;
		double f_aus = isConsuming ? 0.0 : F_AUS_WEIGHT; // Stop trying to "flow" together if eating
        double f_target = isConsuming ? 1.2 : 0.3; // Much stronger pull to the center point if consuming[cite: 1]

        // 2. Calculate individual force vectors
        double[] acc_cohesion = cohesion(allVehicles);
        double[] acc_sep      = separation(allVehicles);
        double[] acc_align    = alignment(allVehicles);
        double[] acc_obs      = obstacleAvoidance(obstacles);
        double[] acc_seek     = seekTarget(target);

        // 3. Combine all forces into a single X and Y sum
        double x = (f_zus * acc_cohesion[0]) +
                (f_sep * acc_sep[0]) +
                (f_aus * acc_align[0]) +
                (f_obs * acc_obs[0]) +
                (f_target * acc_seek[0]);

        double y = (f_zus * acc_cohesion[1]) +
                (f_sep * acc_sep[1]) +
                (f_aus * acc_align[1]) +
                (f_obs * acc_obs[1]) +
                (f_target * acc_seek[1]);

        // 4. Create the final acceleration vector and limit it to max_acc[cite: 1, 2]
        double[] acc_dest = new double[]{x, y};
        return VectorCalculation.truncate(acc_dest, max_acc);
    }

	void move1(ArrayList<Vehicle> allVehicles, ArrayList<Obstacle> obstacles, double[] currentTarget) {
		//STEP 1: Accelaration or Force 
		double[] acc = calculateWeightedAcc(allVehicles, obstacles, currentTarget, false);
	
		//STEP 2: Speed
		vel[0] = vel[0] + acc[0];
		vel[1] = vel[1] + acc[1];

		if (VectorCalculation.length(vel) > 1e-8) {
		    vel = VectorCalculation.normalize(vel);
		}

		vel[0] = vel[0] * max_vel;
		vel[1] = vel[1] * max_vel;

		//STEP 3: Position
		pos[0] = pos[0] + vel[0];
		pos[1] = pos[1] + vel[1];
		
		
		//STEP 4: Box-Simulation
		position_Box();
	}

    // Update the move method in Vehicle.java
    void move(ArrayList<Vehicle> allVehicles, ArrayList<Obstacle> obs, double[] target, boolean isConsuming, boolean isDispersing) {

        // 1. Calculate Acceleration
        double[] acc;

        if (isDispersing) {
            // Move randomly: ignore target, high noise/random force
            double[] randomAcc = new double[]{(Math.random() - 0.5), (Math.random() - 0.5)};
            acc = VectorCalculation.truncate(randomAcc, max_acc);
        } else {
            acc = calculateWeightedAcc(allVehicles, obs, target,  isConsuming);
        }

        // 2. Speed Update with "Braking" logic
        vel[0] = vel[0] + acc[0];
        vel[1] = vel[1] + acc[1];

        if (isConsuming && target != null) {
            double dist = Math.sqrt(Math.pow(target[0] - pos[0], 2) + Math.pow(target[1] - pos[1], 2));
            // If close to target while consuming, slow down to a halt
            if (dist < 10) {
                vel[0] *= 0.5;
                vel[1] *= 0.5;
            }
        }

        // Ensure we don't exceed max_vel, but allow coming to a stop[cite: 1, 2]
        double currentSpeed = VectorCalculation.length(vel);
        if (currentSpeed > max_vel) {
            vel = VectorCalculation.normalize(vel);
            vel[0] *= max_vel;
            vel[1] *= max_vel;
        }

        // 3. Update Position
        pos[0] = pos[0] + vel[0];
        pos[1] = pos[1] + vel[1];

        position_Box();
    }

    double[] seekTarget(double[] target) {
        double[] acc_dest = new double[2];
        if (target == null) return acc_dest;

        double[] vel_dest = new double[2];
        vel_dest[0] = target[0] - pos[0];
        vel_dest[1] = target[1] - pos[1];

        acc_dest = calculateAcc(vel_dest); // Reuse existing utility
        return VectorCalculation.truncate(acc_dest, max_acc);
    }

	double[] obstacleAvoidance(ArrayList<Obstacle> obstacles) {
		double[] acc_total = new double[2];
		// Use a short look-ahead so vehicles begin steering before the body reaches the box.
		double speed = VectorCalculation.length(vel);
		double lookAheadDistance = Math.max(12.0, speed * 18.0);
		double[] lookAhead = new double[]{pos[0], pos[1]};
		if (speed > 1e-8) {
			double[] forward = VectorCalculation.normalize(vel);
			lookAhead[0] += forward[0] * lookAheadDistance;
			lookAhead[1] += forward[1] * lookAheadDistance;
		}

		for (Obstacle obs : obstacles) {
			double ox = obs.position[0];
			double oy = obs.position[1];
			double ow = obs.getObstacle_width();
			double oh = obs.getObstacle_height();

			// Closest point on the rectangle to the look-ahead point.
			double closestX = Math.max(ox, Math.min(lookAhead[0], ox + ow));
			double closestY = Math.max(oy, Math.min(lookAhead[1], oy + oh));

			double dx = lookAhead[0] - closestX;
			double dy = lookAhead[1] - closestY;
			double dist = Math.sqrt(dx * dx + dy * dy);

			// Sensing radius grows with obstacle size so larger boxes are avoided earlier.
			double avoidanceRadius = BASE_AVOIDANCE_RADIUS + Math.max(ow, oh) * 0.5;

			if (dist < avoidanceRadius) {
				double[] pushAway = new double[]{dx, dy};

				if (VectorCalculation.length(pushAway) > 1e-8) {
					pushAway = VectorCalculation.normalize(pushAway);
				} else {
					// If the look-ahead lands inside the obstacle, push away from the obstacle center.
					pushAway[0] = lookAhead[0] - (ox + ow / 2.0);
					pushAway[1] = lookAhead[1] - (oy + oh / 2.0);
					if (VectorCalculation.length(pushAway) > 1e-8) {
						pushAway = VectorCalculation.normalize(pushAway);
					} else {
						// Final fallback: steer sideways relative to current heading.
						pushAway[0] = -vel[1];
						pushAway[1] = vel[0];
						if (VectorCalculation.length(pushAway) > 1e-8) {
							pushAway = VectorCalculation.normalize(pushAway);
						}
					}
				}

				// Stronger response the closer the look-ahead point is to the obstacle.
				double gap = Math.max(avoidanceRadius - dist, 0.0);
				double intensity = Math.pow(gap / Math.max(avoidanceRadius, 1.0), 2) * AVOIDANCE_MULTIPLIER;
				acc_total[0] += pushAway[0] * intensity;
				acc_total[1] += pushAway[1] * intensity;
			}
		}

        // If there is an avoidance force, truncate it to max_acc
        if (VectorCalculation.length(acc_total) > 1e-8) {
            return VectorCalculation.truncate(acc_total, max_acc);
        }
        return new double[]{0, 0};
    }

	public void position_Box() {

        //   If the position is close to the left-edge then
		if (pos[0] < 10) {
			vel[0] = Math.abs(vel[0]);
			pos[0] = pos[0] + vel[0];
		}

        //   If the position is close to the right-edge then velocity is set to negative to move back to the left edge
		if (pos[0] > 1000 * Simulation.pix) {
			vel[0] = -Math.abs(vel[0]);
			pos[0] = pos[0] + vel[0];
		}

        //   If the position is close to the top-edge then
		if (pos[1] < 10) {
			vel[1] = Math.abs(vel[1]);
			pos[1] = pos[1] + vel[1];
		}

        //   If the position is close to the bottom-edge then
		if (pos[1] > 700 * Simulation.pix) {
			vel[1] = -Math.abs(vel[1]);
			pos[1] = pos[1] + vel[1];
		}
	}
}
