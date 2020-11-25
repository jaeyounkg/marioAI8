package ch.idsia.agents;

import java.util.*;
import java.math.*;
import java.lang.Math.*;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.*;
import ch.idsia.evolution.MLP;
import ch.idsia.agents.QLStateAction;

public class QLAgent extends BasicMarioAIAgent implements Agent {
	static String name = "QLAgent";
	// 取り得る行動の数
	public static final int N_ACTIONS = 12;
	// J：ジャンプ S：ファイア R：右 L：左 D：下
	/*
	 * enum Action{ J, S, R, L, D, JS, JR, JL, JD, JSR, JSL, NONE, }
	 */
	public static final int PENALTY_FOR_COLLISION = 1000;

	// 毎フレームもっとも価値の高い行動をするが、確率epsilonで他の行動を等確率で選択
	public static final float epsilon = 0.05f;
	// もっとも良い選択の再現に使用
	private static int frameCounter = 0;
	// 毎エピソードで選択した行動を全フレーム分とっておく
	public static List<Integer> actions;
	// 学習中にもっとも良かった行動群
	public static List<Integer> best;
	// 学習中にもっとも良かったスコア
	public static double bestScore;
	public static int bestLevelSeed;
	// 毎フレームで貪欲な選択をするかどうか
	public static boolean mode = false;
	// 各エピソードで、ある状態である行動を取ったかどうか
	// QLStateActionPairはint4つでstate,cliff,ableToJump,action
	// valueのIntegerはこのQLでは使わない
	public static HashMap<QLStateAction, Integer> selected;
	public static List<QLStateAction> history;
	// learning rate
	public final static double alpha = 0.1;
	// discount factor
	public final static double gamma = 0.5;

	public static MLP QA, QB;
	private static double maxReward = 1;

	private static float prevX, prevY;
	private static int prevCollisionsWithCreatures;
	private static int prevKillsTotal;

	public static void setMode(boolean b) {
		mode = b;
	}

	public static void ini() {
		frameCounter = 0;
		selected.clear();
		history.clear();
		actions.clear();

		// floatMarioPos might not have been initialized yet
		prevX = 0;
		prevY = 0;
		prevCollisionsWithCreatures = 0;
		prevKillsTotal = 0;
	}

	// コンストラクタ
	public QLAgent() {
		super(name);
		selected = new HashMap<QLStateAction, Integer>();
		history = new ArrayList<QLStateAction>();
		QA = new MLP(QLStateAction.N_DIM, 16, 1);
		QB = new MLP(QLStateAction.N_DIM, 16, 1);

		actions = new ArrayList<Integer>();
		best = new ArrayList<Integer>();
	}

	public float[] getPos() {
		return marioFloatPos;
	}

	public boolean[] getAction() {
		float x = marioFloatPos[0];
		float y = marioFloatPos[1];

		// give penalty for collisions
		if (Mario.collisionsWithCreatures > prevCollisionsWithCreatures) {
			giveRewardForRecentActions(8, -0.1);
		}

		// give reward for kills
		if (getKillsTotal > prevKillsTotal) {
			giveRewardForRecentActions(8, 0.001);
		}

		if (x > prevX) {
			// giveRewardForRecentActions(1, 0.005);
		}

		// give penalty for not moving
		if (x == prevX) {
			// giveRewardForRecentActions(1, -0.01);
		}

		if (x < prevX) {
			// giveRewardForRecentActions(1, -0.01);
		}

		clearAction();
		int currAction = 0;
		if (!mode) {
			currAction = chooseAction();
			actions.add(currAction);
			intToAction(currAction);
			final QLState state = getState();
			if (!selected.containsKey(new QLStateAction(state, currAction)))
				selected.put(new QLStateAction(state, currAction), 1);
			else
				selected.put(new QLStateAction(state, currAction),
						selected.get(new QLStateAction(state, currAction)) + 1);
			history.add(new QLStateAction(state, currAction));
		} else {
			// currAction = chooseActionG();
			if (frameCounter < best.size())
				currAction = best.get(frameCounter);
			intToAction(currAction);
		}
		frameCounter++;

		prevX = marioFloatPos[0];
		prevY = marioFloatPos[1];
		prevCollisionsWithCreatures = Mario.collisionsWithCreatures;
		prevKillsTotal = getKillsTotal;

		return action;
	}

	// update Q1 using Q2
	private static void _updateQ(MLP Q1, MLP Q2, QLStateAction prevStateAction, QLState curState, double reward) {
		double maxQ = 0;
		int maxA = 0;
		for (int a = 0; a < QLAgent.N_ACTIONS; ++a) {
			QLStateAction sa = new QLStateAction(curState, a);
			double q = Q1.propagate(sa.toVector())[0];
			if (q > maxQ) {
				maxQ = q;
				maxA = a;
			}
		}
		QLStateAction sa = new QLStateAction(curState, maxA);
		double prevQ2 = Q2.propagate(sa.toVector())[0];
		double prevQ1 = Q1.propagate(prevStateAction.toVector())[0];
		Q1.backPropagate(new double[] { (1 - alpha) * prevQ1 + alpha * (reward + gamma * prevQ2) });
	}

	// update Q(`prevState`, `prevAction`) based on current state `curState`
	public static void updateQ(QLStateAction prevStateAction, QLState curState, double reward) {
		_updateQ(QA, QB, prevStateAction, curState, reward);
		_updateQ(QB, QA, prevStateAction, curState, reward);
	}

	// give rewards for actions taken in the recent `duration` amount of time
	public void giveRewardForRecentActions(int duration, double reward) {
		ListIterator<QLStateAction> iter = history.listIterator(Math.max(0, history.size() - duration - 1));
		if (!iter.hasNext())
			return;
		QLStateAction prevPair = iter.next();
		while (iter.hasNext()) {
			QLStateAction pair = iter.next();
			updateQ(prevPair, pair.getState(), reward);
			prevPair = pair;
		}
		updateQ(prevPair, getState(), reward);
	}

	// give penalty for actions taken in the recent `duration` amount of time
	public void givePenaltyForRecentActions(int duration, double penalty) {
		giveRewardForRecentActions(duration, -penalty);
	}

	// give rewards for the entire state-action pairs
	public void giveRewardForEntireHistory(double reward) {
		giveRewardForRecentActions(history.size(), reward);
	}

	// 障害物を検出し、stateの各bitに0,1で格納
	// ここでマリオが得る情報をほとんど決めている
	// ついでにマリオが地面にいるかも取得
	// 崖検出
	public QLState getState() {
		boolean cliff = true;
		for (int i = 0; i < 10; ++i) {
			if (getReceptiveFieldCellValue(marioEgoRow + i, marioEgoCol + 1) != 0) {
				cliff = false;
				break;
			}
		}

		QLState state = new QLState(levelScene, enemies, marioFloatPos, isMarioOnGround, cliff, isMarioAbleToJump);
		return state;
	}

	// 行動価値関数を基に行動選択
	public int chooseAction() {
		float r = (float) (Math.random());
		int idx = 0;
		if (r < epsilon) {
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
				QLStateAction sa = new QLStateAction(s, i);
				double q = QA.propagate(sa.toVector())[0];
				if (q > max) {
					max = q;
					idx = i;
				}
			}
		}
		return idx;
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
