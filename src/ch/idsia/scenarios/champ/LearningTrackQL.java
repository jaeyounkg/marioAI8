package ch.idsia.scenarios.champ;

import ch.idsia.agents.LearningAgent;
import ch.idsia.agents.LearningWithQL;
import ch.idsia.benchmark.tasks.LearningTask;
import ch.idsia.tools.MarioAIOptions;

public final class LearningTrackQL {
    final static long numberOfTrials = 10000;
    final static boolean scoring = false;

    final static int populationSize = 1000;

    private static void evaluateSubmission(MarioAIOptions marioAIOptions, LearningAgent learningAgent) {
        /* -----------------------学習-------------------------- */

        /* LearningTaskオブジェクトを作成 */
        LearningTask learningTask = new LearningTask(marioAIOptions);

        /* 学習制限回数を取得 */
        learningAgent.setEvaluationQuota(LearningTask.getEvaluationQuota());

        /* 作ったオブジェクトをLearningAgentのTaskとして渡す */
        learningAgent.setLearningTask(learningTask);

        /* LearningAgentの初期化 */
        learningAgent.init();

        // for(int i=0 ; i<LearningTask.getEvaluationQuota() ; i++){ //forで繰り返す???
        // System.out.println("世代 : "+i);
        learningAgent.learn();
        // launches the training process. numberOfTrials happen here
        /*
         * for(int i = 0; i < 64; ++i){ System.out.println(QAgent.qValue[i][0][2]); }
         */

    }

    public static void main(String[] args) {

        final int runWhat = 3;

        /* 学習に用いるAgentを指定 */
        LearningAgent learningAgent;
        switch (runWhat) {
            /* MainTask3.java */
            case 0:
                learningAgent = new LearningWithQL("-lhs off -ltb on -lg off -lb off -ld 1 -ls 0 -le g");
                break;

            /* MainTask4_1.java */
            case 1:
                learningAgent = new LearningWithQL("-lde on -i on -ltb off -ld 2 -ls 0 -le g");
                break;

            /* MainTask4_2.java */
            case 2:
                learningAgent = new LearningWithQL(
                        "-lco off -lb on -le off -lhb off -lg on -ltb on -lhs off -lca on -lde on -ld 5 -ls 133829");
                break;

            /* MainTask4_3.java */
            default:
                learningAgent = new LearningWithQL("-lde on -i off -ld 30 -ls 133434 -lhb on");
        }

        System.out.println("main.learningAgent = " + learningAgent);

        /* パラメータを設定する */
        MarioAIOptions marioAIOptions = new MarioAIOptions(args);
        // LearningAgent learningAgent = new MLPESLearningAgent(); // Learning track
        // competition entry goes here
        evaluateSubmission(marioAIOptions, learningAgent);
        // replay("MonteCarloMainTask3.txt",marioAIOptions,(LearningWithQL2)learningAgent);

        System.exit(0);
    }
}
