package ch.idsia.agents;

import java.util.*;
import java.math.*;
import java.lang.Math.*;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.*;
import ch.idsia.evolution.MLP;
import ch.idsia.agents.DQLStateAction;

public class DQLAgent extends BasicMarioAIAgent implements Agent {
	static String name = "DQLAgent";
	public static final int PENALTY_FOR_COLLISION = 1000;

	// 毎フレームもっとも価値の高い行動をするが、確率epsilonで他の行動を等確率で選択
	public static final float epsilon = 0.035f;
	// もっとも良い選択の再現に使用
	private static int frameCounter = 0;
	// 毎エピソードで選択した行動を全フレーム分とっておく
	public static List<DQLAction> actions;
	// 学習中にもっとも良かった行動群
	public static List<DQLAction> bestActions;
	// 学習中にもっとも良かったスコア
	public static double bestScore;
	public static int bestLevelSeed;
	// 毎フレームで貪欲な選択をするかどうか
	public static boolean mode = false;
	// 各エピソードで、ある状態である行動を取ったかどうか
	// QLStateActionはint4つでstate,cliff,ableToJump,action
	// valueのIntegerはこのQLでは使わない
	public static HashMap<DQLStateAction, Integer> selected;
	public static List<DQLStateAction> history;
	// learning rate
	public final static double alpha = 0.1;
	// discount factor
	public final static double gamma = 0.5;

	public static MLP mainQ, predQ;
	private final static int C = 500;
	private static int qUpdateCount = 0;

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
	public DQLAgent() {
		super(name);
		selected = new HashMap<DQLStateAction, Integer>();
		history = new ArrayList<DQLStateAction>();
		predQ = new MLP(DQLState.N_DIM, 16, DQLAction.N_DIM);
		mainQ = predQ.copy();

		actions = new ArrayList<DQLAction>();
		bestActions = new ArrayList<DQLAction>();
	}

	public float[] getPos() {
		return marioFloatPos;
	}

	public boolean[] getAction() {
		float x = marioFloatPos[0];
		float y = marioFloatPos[1];

		// give penalty for collisions
		if (Mario.collisionsWithCreatures > prevCollisionsWithCreatures) {
			// giveRewardForRecentActions(8, -0.5);
		}

		// give reward for kills
		if (getKillsTotal > prevKillsTotal) {
			// giveRewardForRecentActions(8, 0.001);
		}

		if (x > prevX) {
			// giveRewardForRecentActions(1, 0.005);
		}

		// give penalty for not moving
		if (x == prevX) {
			// giveRewardForRecentActions(1, -0.001);
		}

		if (x < prevX) {
			// giveRewardForRecentActions(1, -0.01);
		}

		clearAction();
		DQLAction currAction;
		if (!mode) {
			currAction = chooseAction();
			actions.add(currAction);
			action = currAction.action.clone();
			final DQLState currState = getState();
			if (!selected.containsKey(new DQLStateAction(currState, currAction)))
				selected.put(new DQLStateAction(currState, currAction), 1);
			else
				selected.put(new DQLStateAction(currState, currAction),
						selected.get(new DQLStateAction(currState, currAction)) + 1);
			history.add(new DQLStateAction(currState, currAction));
		} else {
			if (frameCounter < bestActions.size()) {
				currAction = bestActions.get(frameCounter);
				action = currAction.action.clone();
			}
		}
		frameCounter++;

		prevX = marioFloatPos[0];
		prevY = marioFloatPos[1];
		prevCollisionsWithCreatures = Mario.collisionsWithCreatures;
		prevKillsTotal = getKillsTotal;

		return action;
	}

	// update Q(`prevState`, `prevAction`) based on current state `curState`
	public static void updateQ(DQLStateAction prevStateAction, DQLState curState, double reward) {
		double maxQ = 0;
		int maxA = 0;

		double[] qs = mainQ.propagate(curState.toVector());
		for (int a = 0; a < DQLAction.N_DIM; ++a) {
			if (qs[a] > maxQ) {
				maxQ = qs[a];
				maxA = a;
			}
		}
		double[] targetQs = predQ.propagate(curState.toVector());
		targetQs[maxA] = Math.min(1, reward + gamma * maxQ);
		predQ.backPropagate(targetQs);

		++qUpdateCount;
		if (qUpdateCount >= C) {
			mainQ = predQ.copy();
			qUpdateCount = 0;
		}
	}

	// give rewards for actions taken in the recent `duration` amount of time
	public void giveRewardForRecentActions(int duration, double reward) {
		ListIterator<DQLStateAction> iter = history.listIterator(Math.max(0, history.size() - duration - 1));
		if (!iter.hasNext())
			return;
		DQLStateAction prevPair = iter.next();
		while (iter.hasNext()) {
			DQLStateAction pair = iter.next();
			updateQ(prevPair, pair.getState(), reward);
			prevPair = pair;
		}
		updateQ(prevPair, getState(), reward);
	}

	// give rewards for the entire state-action pairs
	public void giveRewardForEntireHistory(double reward) {
		giveRewardForRecentActions(history.size(), reward);
	}

	// 障害物を検出し、stateの各bitに0,1で格納
	// ここでマリオが得る情報をほとんど決めている
	// ついでにマリオが地面にいるかも取得
	// 崖検出
	public DQLState getState() {
		boolean cliff = true;
		for (int i = 0; i < 10; ++i) {
			if (getReceptiveFieldCellValue(marioEgoRow + i, marioEgoCol + 1) != 0) {
				cliff = false;
				break;
			}
		}

		DQLState state = new DQLState(levelScene, enemies, marioFloatPos, isMarioOnGround, cliff, isMarioAbleToJump);
		return state;
	}

	// 行動価値関数を基に行動選択
	public DQLAction chooseAction() {
		float r = (float) (Math.random());
		int idx = 0;
		if (r < epsilon) {
			float sum = 0;
			float d = epsilon / (float) DQLAction.N_DIM;
			sum += d;
			while (sum < r) {
				sum += d;
				idx++;
			}
		} else {
			predQ.propagate(getState().toVector());
			idx = predQ.getOutputMaxElem();
		}
		return new DQLAction(idx);
	}

	// 行動選択前にactionを一旦全部falseにする
	public void clearAction() {
		for (int i = 0; i < Environment.numberOfKeys; ++i) {
			action[i] = false;
		}
	}
}
