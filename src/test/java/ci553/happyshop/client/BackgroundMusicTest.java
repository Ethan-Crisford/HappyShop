package ci553.happyshop.client;

import org.junit.jupiter.api.Test;

class BackgroundMusicTest
{
    @Test
    void bgmResourceExists()
    {
        var url = BackgroundMusicTest.class.getResource("/audio/bgm.mp3");
    }
}