 /*
  *  Copyright (C) 2022 github.com/REAndroid
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.reandroid.lib.arsc.array;

import com.reandroid.lib.arsc.base.BlockArray;
import com.reandroid.lib.arsc.chunk.SpecBlock;

public class SpecBlockArray extends BlockArray<SpecBlock> {
    public SpecBlockArray(){
        super();
    }
    @Override
    public SpecBlock newInstance() {
        return new SpecBlock();
    }

    @Override
    public SpecBlock[] newInstance(int len) {
        return new SpecBlock[len];
    }

    @Override
    protected void onRefreshed() {

    }
}
