package ch.idsia.agents;

import java.util.*;
import java.math.*;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.*;
import ch.idsia.agents.MCStateActionPair;

public class MCAgent extends BasicMarioAIAgent implements Agent {
	static String name = "MCAgent";
	// 取り得る行動の数
	public static final int N_ACTIONS = 12;
	// J：ジャンプ S：ファイア R：右 L：左 D：下
	/*
	 * enum Action{ J, S, R, L, D, JS, JR, JL, JD, JSR, JSL, NONE, }
	 */
	public static final int PENALTY_FOR_COLLISION = 1000;

	// 毎フレームもっとも価値の高い行動をするが、確率epsilonで他の行動を等確率で選択
	public static final float epsilon = 0.005f;
	// もっとも良い選択の再現に使用
	private static int frameCounter = 0;
	// 毎エピソードで選択した行動を全フレーム分とっておく
	public static List<Integer> actions;
	// 学習中にもっとも良かった行動群
	public static List<Integer> best;
	public static float[][] bestSumValue;
	public static int[][] bestNum;
	// 学習中にもっとも良かったスコア
	public static float bestScore;
	// 毎フレームで貪欲な選択をするかどうか
	public static boolean mode = false;
	// 各エピソードで、ある状態である行動を取ったかどうか
	// MCStateActionPairはint4つでstate,cliff,ableToJump,action
	// valueのIntegerはこのMCでは使わない
	public static HashMap<MCStateActionPair, Integer> selected;
	public static List<MCStateActionPair> history;
	// 行動価値関数 これを基に行動を決める
	public static float[][] qValue;
	// 各状態行動対におけるそれまで得た報酬の合計
	public static float[][] sumValue;
	// ある状態である行動を取った回数. initialized to 1
	public static int[][] num;

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
	public MCAgent() {
		super(name);
		qValue = new float[MCState.N_STATES][N_ACTIONS];
		sumValue = new float[MCState.N_STATES][N_ACTIONS];
		num = new int[MCState.N_STATES][N_ACTIONS];
		selected = new HashMap<MCStateActionPair, Integer>();
		history = new ArrayList<MCStateActionPair>();
		Random random = new Random();
		for (int i = 0; i < MCState.N_STATES; ++i) {
			for (int j = 0; j < N_ACTIONS; ++j) {
				qValue[i][j] = 0.0f;
				// 一応全パターンは1回は試したいのである程度の値は持たせる
				// if (j == 2 || j == 6 || j == 9)
				sumValue[i][j] = 4000f;
				// else
				// sumValue[i][j] = 1000f;
				num[i][j] = 1;
			}
		}

		actions = new ArrayList<Integer>();
		best = new ArrayList<Integer>();
	}

	public boolean[] getAction() {
		clearAction();
		int currAction = 0;

		if (Mario.collisionsWithCreatures > prevCollisionsWithCreatures) {
			givePenaltyForRecentActions(6, 100);
		}
		if (getKillsTotal > prevKillsTotal) {
			giveRewardForRecentActions(6, 1);
		}

		if (!mode) {
			currAction = chooseAction();
			actions.add(currAction);
			intToAction(currAction);
			final MCState state = getState();
			if (!selected.containsKey(new MCStateActionPair(state, currAction)))
				selected.put(new MCStateActionPair(state, currAction), 1);
			else
				selected.put(new MCStateActionPair(state, currAction),
						selected.get(new MCStateActionPair(state, currAction)) + 1);
			history.add(new MCStateActionPair(state, currAction));
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

	public static void updateSumValue(int state, int action, float delta) {
		sumValue[state][action] += delta;
		if (sumValue[state][action] < 0)
			sumValue[state][action] = 0;
		qValue[state][action] = sumValue[state][action] / num[state][action];
	}

	// give rewards for actions taken in the recent `duration` amount of time
	public static void giveRewardForRecentActions(int duration, float reward) {
		ListIterator<MCStateActionPair> iter = history.listIterator(history.size());
		for (int i = 0; i < duration && iter.hasPrevious(); ++i) {
			MCStateActionPair key = iter.previous();
			int s = key.getState().toInt();
			int a = key.getAction();
			updateSumValue(s, a, reward);
		}
		return;
	}

	// give penalty for actions taken in the recent `duration` amount of time
	public static void givePenaltyForRecentActions(int duration, float penalty) {
		giveRewardForRecentActions(duration, -penalty);
	}

	// number of state-action pairs that have ever been visited
	public static int getNumberOfVisitedSAs() {
		int n = 0;
		for (int i = 0; i < MCState.N_STATES; ++i) {
			for (int j = 0; j < N_ACTIONS; ++j) {
				n += (num[i][j] > 1) ? 1 : 0;
			}
		}
		return n;
	}

	// get max(num)
	public static int getMaxNum() {
		int M = 0;
		for (int i = 0; i < MCState.N_STATES; ++i) {
			for (int j = 0; j < N_ACTIONS; ++j) {
				if (MCAgent.num[i][j] > M)
					M = MCAgent.num[i][j];
			}
		}
		return M;
	}

	// 行動価値関数を取得
	public static float[][] getQ() {
		return qValue;
	}

	// 行動価値関数を取得
	// 学習した後に再現で使う
	public static void setQ(float[][] q) {
		qValue = q;
	}

	// 障害物を検出し、stateの各bitに0,1で格納
	// ここでマリオが得る情報をほとんど決めている
	// ついでにマリオが地面にいるかも取得
	// 崖検出
	public MCState getState() {
		boolean cliff = true;
		for (int i = 0; i < 10; ++i) {
			if (getReceptiveFieldCellValue(marioEgoRow + i, marioEgoCol + 1) != 0) {
				cliff = false;
				break;
			}
		}

		MCState state = new MCState(levelScene, enemies, isMarioOnGround, cliff, isMarioAbleToJump);
		return state;
	}

	// ソフトマックス手法
	public int chooseActionS() {
		float sum = 0.0f;
		int idx = 0;
		for (int i = 0; i < N_ACTIONS; ++i) {
			sum += Math.pow(Math.E, qValue[getState().toInt()][i] / 25f);
		}
		float r = (float) (Math.random());
		float f = 0.0f;
		for (int i = 0; i < N_ACTIONS; ++i) {
			f += Math.pow(Math.E, qValue[getState().toInt()][i] / 25f) / sum;
			if (f > r) {
				idx = i;
				break;
			}
		}
		return idx;
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
			float max = -Float.MAX_VALUE;
			int s = getState().toInt();
			for (int i = 0; i < N_ACTIONS; ++i) {
				float q = qValue[s][i];
				if (q > max) {
					max = q;
					idx = i;
				}
			}
		}
		return idx;
	}

	// 貪欲に行動を選択
	public int chooseActionG() {
		int idx = 0;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < N_ACTIONS; ++i) {
			float q = qValue[getState().toInt()][i];
			if (q > max) {
				max = q;
				idx = i;
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
