package ch.idsia.agents;

import java.util.*;
import java.util.Map.Entry;
import java.math.*;
import java.lang.Math.*;
import java.io.*;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.*;
import ch.idsia.agents.QLStateAction;

public class QLAgent extends BasicMarioAIAgent implements Agent {
	static String name = "QLAgent";
	// 取り得る行動の数
	public static final int N_ACTIONS = 12;
	// J：ジャンプ S：ファイア R：右 L：左 D：下
	/*
	 * enum Action{ J, S, R, L, D, JS, JR, JL, JD, JSR, JSL, NONE, }
	 */

	// 毎フレームもっとも価値の高い行動をするが、確率epsilonで他の行動を等確率で選択
	private boolean explorativeMode = false;
	public float epsilon = 0.10f;
	private float maxEpsilon = 0.50f;
	private float explorativeMaxDist = 4096f;

	// when replaying, agent only follows `actions`
	private boolean replayingMode = false;
	private int frameCounter = 0;
	public List<Integer> actions;

	public static final int UPPER_TICKS_FOR_STAYING_STILL = 600;
	public List<QLStateAction> history;
	// 行動価値関数 これを基に行動を決める
	public QLQFunction Q;
	public final double INITIAL_Q = 0;
	// learning rate
	private double alpha = 0.1;
	// discount factor
	private double gamma = 0.5;

	private float prevX, prevY;
	private int prevCollisionsWithCreatures;
	private int prevKillsTotal;

	public void ini(boolean explorativeMode, boolean replayingMode) {
		frameCounter = 0;
		history.clear();
		this.explorativeMode = explorativeMode;
		this.replayingMode = replayingMode;
		if (!replayingMode)
			actions.clear();

		// floatMarioPos might not have been initialized yet
		prevX = 0;
		prevY = 0;
		prevCollisionsWithCreatures = 0;
		prevKillsTotal = 0;
	}

	public void setExplorativeMaxDist(float maxDist) {
		explorativeMaxDist = maxDist;
	}

	// コンストラクタ
	public QLAgent() {
		super(name);
		Q = new QLQFunction();
		history = new ArrayList<QLStateAction>();
		actions = new ArrayList<Integer>();
	}

	public boolean[] getAction() {
		float marioX = marioFloatPos[0];
		float marioY = marioFloatPos[1];

		if (explorativeMode) {
			// give penalty for collisions
			if (Mario.collisionsWithCreatures > prevCollisionsWithCreatures) {
				giveRewardForRecentActions(8, 0);
			}

			// give reward for kills
			if (getKillsTotal > prevKillsTotal) {
				// giveRewardForRecentActions(16, 200);
			}

			if (marioX == prevX) {
				// giveRewardForRecentActions(1, -1);
			}

			if (history.size() > 1) {
				final QLStateAction lastSA = history.get(history.size() - 1);
				lastSA.state.assertInformationIsAvailable();

				// give reward for jumping to higher location
				if (isMarioOnGround && !lastSA.state.onGround) {
					ListIterator<QLStateAction> iter = history.listIterator(history.size());
					for (int duration = 1; iter.hasPrevious(); ++duration) {
						final QLStateAction sa = iter.previous();
						sa.state.assertInformationIsAvailable();
						if (sa.state.onGround) {
							if (sa.state.marioY > marioY) {
								giveRewardForRecentActions(duration, (sa.state.marioY - marioY) / 16 * 1000);
							}
							break;
						}
					}
				}

				// give penalty for staying still for too long
				if (history.size() % UPPER_TICKS_FOR_STAYING_STILL == 0) {
					ListIterator<QLStateAction> iter = history.listIterator(history.size());
					for (int duration = 1; iter.hasPrevious(); ++duration) {
						QLStateAction sa = iter.previous();
						sa.state.assertInformationIsAvailable();
						if (sa.state.marioX < marioX - 16f || marioX + 16f < sa.state.marioX)
							break;
						if (duration >= UPPER_TICKS_FOR_STAYING_STILL) {
							giveRewardForRecentActions(duration, 0);
							break;
						}
					}
				}
			}
		}

		clearAction();
		int currAction = 0;
		if (!replayingMode) {
			currAction = chooseAction();
			intToAction(currAction);
			actions.add(currAction);
			history.add(new QLStateAction(getState(), currAction));
		} else {
			if (frameCounter < actions.size())
				currAction = actions.get(frameCounter);
			intToAction(currAction);
		}
		frameCounter++;

		prevX = marioX;
		prevY = marioY;
		prevCollisionsWithCreatures = Mario.collisionsWithCreatures;
		prevKillsTotal = getKillsTotal;

		return action;
	}

	// 行動価値関数を基に行動選択
	public int chooseAction() {
		float r = (float) (Math.random());
		// float epsilon = maxEpsilon * (float) Math.pow(marioFloatPos[0] /
		// explorativeMaxDist, 3);
		int idx = 0;
		if (explorativeMode && r < epsilon) {
			float sum = 0;
			float d = epsilon / (float) N_ACTIONS;
			sum += d;
			while (sum < r) {
				sum += d;
				idx++;
			}
		} else {
			double max = -Double.MAX_VALUE;
			QLState s = getState();
			for (int i = 0; i < N_ACTIONS; ++i) {
				double q = Q.getOrDefault(new QLStateAction(s, i), INITIAL_Q);
				if (q > max) {
					max = q;
					idx = i;
				}
			}
		}
		return idx;
	}

	// update Q(`prevState`, `prevAction`) based on current state `curState`
	public void updateQ(QLStateAction prevStateAction, QLState curState, double reward) {
		if (Double.isNaN(reward))
			return;

		double maxQ = 0;
		for (int a = 0; a < QLAgent.N_ACTIONS; ++a) {
			maxQ = Math.max(maxQ, Q.getOrDefault(new QLStateAction(curState, a), INITIAL_Q));
		}

		if (Q.containsKey(prevStateAction)) {
			double newQ = (1 - alpha) * Q.get(prevStateAction) + alpha * (reward + gamma * maxQ);
			Q.put(prevStateAction, newQ);
		} else {
			Q.put(prevStateAction, reward + gamma * maxQ);
		}
	}

	// give rewards for actions taken in the recent `duration` amount of time
	public void giveRewardForRecentActions(int duration, double reward) {
		ListIterator<QLStateAction> iter = history.listIterator(history.size());
		if (!iter.hasPrevious())
			return;
		QLStateAction curPair = iter.previous();
		updateQ(curPair, getState(), reward);
		for (int i = 1; iter.hasPrevious() && i < duration; ++i) {
			QLStateAction prevPair = iter.previous();
			updateQ(prevPair, curPair.state, reward);
			curPair = prevPair;
		}
	}

	// give rewards for the entire state-action pairs
	public void giveRewardForEntireHistory(double reward) {
		giveRewardForRecentActions(history.size(), reward);
	}

	public static void saveModelToFile(QLQFunction Q, String filename) throws IOException {
		// 学習した行動価値関数を書き込み
		File f = new File(filename);
		f.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write(String.valueOf(Q.size()));
		bw.newLine();
		for (Entry<QLStateAction, Double> entry : Q.entrySet()) {
			QLStateAction sa = entry.getKey();
			Double q = entry.getValue();
			bw.write(String.valueOf(sa.toLong()));
			bw.newLine();
			bw.write(String.valueOf(q));
			bw.newLine();
		}
		bw.close();
	}

	public void loadModelFromFile(String filename) throws NumberFormatException, IOException {
		Q = new QLQFunction();
		File f = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String s = br.readLine();
		int n = Integer.parseInt(s);
		for (int i = 0; i < n; ++i) {
			s = br.readLine();
			long key = Long.parseLong(s);
			s = br.readLine();
			Double value = Double.parseDouble(s);
			if (value.isNaN())
				value = INITIAL_Q;
			Q.put(new QLStateAction(key), value);
		}
		br.close();
	}

	// 障害物を検出し、stateの各bitに0,1で格納
	// ここでマリオが得る情報をほとんど決めている
	// ついでにマリオが地面にいるかも取得
	// 崖検出
	public QLState getState() {
		return new QLState(levelScene, enemies, marioFloatPos, isMarioOnGround, isMarioAbleToJump);
	}

	// 行動選択前にactionを一旦全部falseにする
	public void clearAction() {
		for (int i = 0; i < Environment.numberOfKeys; ++i) {
			action[i] = false;
		}
	}

	// int(0-11)をacitonにする
	public void intToAction(int n) {
		if (n == 0 || (n > 4 && n < 11))
			action[Mario.KEY_JUMP] = true;
		if (n == 1 || n == 5 || n == 9 || n == 10)
			action[Mario.KEY_SPEED] = true;
		if (n == 2 || n == 6 || n == 9)
			action[Mario.KEY_RIGHT] = true;
		if (n == 3 || n == 7 || n == 10)
			action[Mario.KEY_LEFT] = true;
		if (n == 4 || n == 8)
			action[Mario.KEY_DOWN] = true;
	}
}
