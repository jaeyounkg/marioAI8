package ch.idsia.agents;

import java.io.*;
import java.util.*;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.benchmark.tasks.LearningTask;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;
import ch.idsia.utils.wox.serial.Easy;
import ch.idsia.agents.QLAgent;
import ch.idsia.agents.QLStateAction;

public class LearningWithQL implements LearningAgent {
	private final String FILENAME = "LearnedQLModel.txt";

	private QLAgent agent;
	private String name = "LearningWithQL";
	// 目標値(4096.0はステージの右端)
	private float goal = 4096.0f;
	private String args;
	// 試行回数
	private int numOfTrial = 50000;

	private double allTimeBestScore = 1;

	// コンストラクタ
	public LearningWithQL(String args) {
		this.args = args;
		agent = new QLAgent();
		QLAgent.bestScore = 0;

		try {
			File f = new File(FILENAME);
			BufferedReader br = new BufferedReader(new FileReader(f));
			for (int i = 0; i < QLState.N_STATES; ++i) {
				for (int j = 0; j < QLAgent.N_ACTIONS; ++j) {
					String s = br.readLine();
					Double v = Double.parseDouble(s);
					if (v != QLAgent.INITIAL_Q) {
						QLAgent.Q.put(new QLStateAction(new QLState(i), j), v);
					}
				}
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	// 学習部分
	// 学習してその中でもっとも良かったものをリプレイ
	public void learn() {
		final int SHOW_INTERVALS = 1000;

		long startTime = System.currentTimeMillis();
		for (int nt = 0; nt < numOfTrial; ++nt) {
			// 目標値までマリオが到達したらshowして終了
			if (run() >= 4096.0f) {
				show();
				break;
			}
			if (nt % SHOW_INTERVALS == SHOW_INTERVALS - 1) {

				long endTime = System.currentTimeMillis();

				System.out.println("time for " + SHOW_INTERVALS + " plays:" + (endTime - startTime) + "(ms)");
				System.out.println("current best score: " + QLAgent.bestScore);
				System.out.println("all time best score: " + allTimeBestScore);
				QLAgent.bestScore = 0;

				show();

				try {
					// 学習した行動価値関数を書き込み
					File f = new File(FILENAME);
					f.createNewFile();
					BufferedWriter bw = new BufferedWriter(new FileWriter(f));
					for (int i = 0; i < QLState.N_STATES; ++i) {
						for (int j = 0; j < QLAgent.N_ACTIONS; ++j) {
							bw.write(String.valueOf(QLAgent.bestQ[i][j]));
							bw.newLine();
						}
					}
					bw.close();
				} catch (IOException e) {
					System.out.println(e);
				}

				startTime = System.currentTimeMillis();
			}

		}

	}

	// リプレイ
	public void show() {
		QLAgent.ini();
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		QLAgent.setMode(true);

		/* プレイ画面出力するか否か */
		marioAIOptions.setVisualization(true);
		/* QLAgentをセット */
		marioAIOptions.setAgent(agent);
		marioAIOptions.setFPS(100);
		basicTask.setOptionsAndReset(marioAIOptions);

		if (!basicTask.runSingleEpisode(1)) {
			System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		// 報酬取得
		float reward = evaluationInfo.distancePassedPhys;
		System.out.println("報酬は" + reward);
	}

	// 学習
	// 画面に表示はしない
	public double run() {
		long startTime = System.currentTimeMillis();

		QLAgent.ini();
		/* QLAgentをプレイさせる */
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		QLAgent.setMode(false);

		/* プレイ画面出力するか否か */
		marioAIOptions.setVisualization(false);
		/* QLAgentをセット */
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if (!basicTask.runSingleEpisode(1)) {
			System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		// 報酬取得
		double score = evaluationInfo.distancePassedPhys;

		/* Normalize num values when they are too large and slowing down learning */
		// if (QLAgent.getMaxNum() > 10000) {
		// for (int i = 0; i < QLState.N_STATES; ++i) {
		// for (int j = 0; j < QLAgent.N_ACTIONS; ++j) {
		// if (QLAgent.num[i][j] > 3) {
		// QLAgent.num[i][j] /= 2;
		// QLAgent.sumValue[i][j] /= 2;
		// }
		// }
		// }
		// }

		// ベストスコアが出たら更新
		allTimeBestScore = Math.max(allTimeBestScore, score);
		if (score > QLAgent.bestScore) {
			QLAgent.bestScore = score;
			QLAgent.best = new ArrayList<Integer>(QLAgent.actions);
			QLAgent.bestQ = new double[QLState.N_STATES][QLAgent.N_ACTIONS];
			for (int i = 0; i < QLState.N_STATES; ++i) {
				QLState state = new QLState(i);
				for (int j = 0; j < QLAgent.N_ACTIONS; ++j) {
					QLStateAction pair = new QLStateAction(state, j);
					QLAgent.bestQ[i][j] = QLAgent.Q.getOrDefault(pair, QLAgent.INITIAL_Q);
				}
			}
		}

		// double reward = (2 * Math.pow(score / allTimeBestScore, 2) - 0.5) * 1000;
		double reward = Math.pow(score / allTimeBestScore, 2) * 1000;
		agent.giveRewardForEntireHistory(reward);

		long endTime = System.currentTimeMillis();
		System.out.println(score + " r:" + (int) reward + " s:" + QLAgent.Q.size() + " g:" + evaluationInfo.timeSpent
				+ "(s) t:" + (endTime - startTime) + "(ms)");

		return score;
	}

	////////////////////////////// ここからは必要なし//////////////////////////////
	@Override
	public boolean[] getAction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void integrateObservation(Environment environment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void giveIntermediateReward(float intermediateReward) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setObservationDetails(int rfWidth, int rfHeight, int egoRow, int egoCol) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void giveReward(float reward) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newEpisode() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLearningTask(LearningTask learningTask) {
		// TODO Auto-generated method stub

	}

	@Override
	public Agent getBestAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEvaluationQuota(long num) {
		// TODO Auto-generated method stub

	}
}